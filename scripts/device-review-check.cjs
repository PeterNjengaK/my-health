// Run only against the isolated .reviewcheck install, never the owner's production app.
const fs=require('node:fs'),assert=require('node:assert/strict');
(async()=>{
  const targets=await(await fetch('http://127.0.0.1:9222/json')).json();
  const target=targets.find(t=>t.url.includes('app.health.local'));
  assert(target,'Test WebView must be open');
  const ws=new WebSocket(target.webSocketDebuggerUrl);
  await new Promise((r,j)=>{ws.onopen=r;ws.onerror=j});
  const code=fs.readFileSync('.release/review-access-code.txt','utf8').trim();
  const expression=`(()=>{
    const check=(ok,m)=>{if(!ok)throw new Error(m)};
    close();
    check(!JSON.parse(Health.state()).pro.reviewer,'Clean install has no grant');
    check(Health.reviewAccess('invalid').length>0,'Invalid grant rejected');
    check(!JSON.parse(Health.state()).pro.reviewer,'Invalid grant grants no access');
    showPro();
    document.querySelector('[data-action="review-access"]').click();
    check(!!document.querySelector('#review-access-form'),'Review form available');
    document.querySelector('#review-code').value=${JSON.stringify(code)};
    document.querySelector('#review-access-form').dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));
    check(JSON.parse(Health.state()).pro.reviewer,'Native signature grants review access');
    check(JSON.parse(Health.state()).pro.owned===false,'Grant does not fake a purchase');
    check(document.querySelector('[data-action="buy-pro"]').disabled,'Review grant disables duplicate purchase');
    check(document.querySelector('[data-action="buy-pro"]').textContent==='Review access active','Review state clearly labeled');
    state.pro={...state.pro,owned:true};render();
    check(document.querySelector('[data-action="buy-pro"]').textContent==='Pro unlocked','Pro sheet refreshes without reopening');
    return {passed:9,version:state.version};
  })()`;
  ws.onmessage=e=>{const m=JSON.parse(e.data);if(m.id!==1)return;if(m.result?.exceptionDetails){console.error(m.result.exceptionDetails.exception?.description||'Device test failed');process.exitCode=1}else console.log(JSON.stringify(m.result.result.value));ws.close()};
  ws.send(JSON.stringify({id:1,method:'Runtime.evaluate',params:{expression,returnByValue:true}}));
})().catch(e=>{console.error(e.message);process.exitCode=1});
