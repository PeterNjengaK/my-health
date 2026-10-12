// Run after adb forward tcp:9222 localabstract:webview_devtools_remote_<app PID>.
// Uses temporary records and removes them in finally; never fabricates steps.
const fs=require('node:fs'),assert=require('node:assert/strict');
(async()=>{
 const targets=await(await fetch('http://127.0.0.1:9222/json')).json();
 const target=targets.find(t=>t.url.includes('app.health.local'));
 assert(target,'My Health WebView must be open');
 const ws=new WebSocket(target.webSocketDebuggerUrl);await new Promise((r,j)=>{ws.onopen=r;ws.onerror=j});
 let id=0;const pending=new Map(),errors=[];
 ws.onmessage=e=>{let m=JSON.parse(e.data);if(m.method==='Runtime.exceptionThrown')errors.push(m.params.exceptionDetails.text);if(pending.has(m.id)){const p=pending.get(m.id);pending.delete(m.id);m.error?p.reject(m.error):p.resolve(m.result)}};
 const call=(method,params={})=>new Promise((resolve,reject)=>{const key=++id;pending.set(key,{resolve,reject});ws.send(JSON.stringify({id:key,method,params}));setTimeout(()=>{if(pending.has(key)){pending.delete(key);reject(new Error('CDP timeout'))}},10000).unref()});
 const evalJS=async expression=>{const r=await call('Runtime.evaluate',{expression,returnByValue:true,awaitPromise:true});assert(!r.exceptionDetails,JSON.stringify(r.exceptionDetails));return r.result.value};
 await call('Runtime.enable');await call('Page.enable');
 const before=JSON.parse(await evalJS('Health.state()'));
 const testIds=[];
 try {
  for(const [route,label] of [['home','Every step counts.'],['activity','Your week in steps'],['journal','Blood glucose'],['insights','Habit averages'],['settings','Local storage. No account.']]){
   await evalJS(`go(${JSON.stringify(route)})`);assert((await evalJS('document.body.innerText')).includes(label),route+' renders');
  }
  for(const type of ['water','weight','sleep','workout','food','bloodpressure','heartrate','glucose','temperature','mood','symptom','medication','period','note']){
   await evalJS(`add(${JSON.stringify(type)})`);assert.equal(await evalJS("document.querySelector('#entry').dataset.type"),type);await evalJS('close()');
   const record={type,date:before.today,label:'SMOKE TEST <script>alert(1)</script>',value:type==='symptom'?'Mild':type==='note'?'Temporary test record':'1',note:'Temporary check; removed automatically',time:type==='medication'?'23:59':'',diastolic:type==='bloodpressure'?'80':undefined};
   const result=await evalJS(`Health.add(${JSON.stringify(JSON.stringify(record))})`);assert.equal(result,'');
   const snapshot=JSON.parse(await evalJS('Health.state()'));const added=snapshot.entries.at(-1);testIds.push(added.id);assert.equal(added.type,type);
  }
  await evalJS('refresh();go("journal")');assert(!(await evalJS('document.querySelector("#app").innerHTML')).includes('<script>alert(1)</script>'),'records escape HTML');
  assert.equal(JSON.parse(await evalJS('Health.state()')).entries.length,before.entries.length+14);
  assert.equal(JSON.parse(await evalJS('Health.state()')).steps[before.today]||0,before.steps[before.today]||0,'logs do not invent steps');
  for(const entryId of testIds)await evalJS(`Health.delete(${JSON.stringify(entryId)})`);
  testIds.length=0;
  assert.equal(JSON.parse(await evalJS('Health.state()')).entries.length,before.entries.length,'temporary records removed');
  assert.equal(await evalJS('Health.add("{}")'),'Could not save entry');
  await evalJS('refresh();go("home")');
  assert.equal(await evalJS('window.scrollY'),0);
  assert.equal(errors.length,0,'no JavaScript exceptions');
  const shot=await call('Page.captureScreenshot',{format:'png'});fs.mkdirSync('artifacts',{recursive:true});fs.writeFileSync('artifacts/device-home.png',Buffer.from(shot.data,'base64'));
  console.log('PASS: five screens, fourteen log forms, native persistence, HTML escaping, cleanup, validation, scroll position and runtime checks.');
 } finally {for(const entryId of testIds)await evalJS(`Health.delete(${JSON.stringify(entryId)})`);await evalJS('refresh();go("home")');ws.close()}
})().catch(e=>{console.error(e);process.exitCode=1});
