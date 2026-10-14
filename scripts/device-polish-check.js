(() => {
  const original=JSON.parse(Health.state());
  let count=0;
  const check=(ok,message)=>{if(!ok)throw new Error(message);count++;};
  try {
    state=original;go('settings');
    check(!!$('you-weight')&&!!$('you-distance'),'Visible independent units');
    $('you-weight').value='lbs';$('you-weight').dispatchEvent(new Event('change',{bubbles:true}));
    $('you-distance').value='mi';$('you-distance').dispatchEvent(new Event('change',{bubbles:true}));
    check(JSON.parse(Health.state()).weightUnit==='lbs'&&JSON.parse(Health.state()).distanceUnit==='mi','Units persist natively');
    check(home().includes('mi · estimated'),'Home miles');
    add('weight');check(document.querySelector('label[for=value]').textContent.includes('lbs'),'Weight entry pounds');close();
    state.workout={active:true,distance:1609.344,elapsed:600000,route:[],label:'Test'};
    check(gpsCard().includes('1.00')&&gpsCard().includes('min / mi'),'GPS distance and pace miles');
    state=JSON.parse(Health.state());
    showSetup();
    check($('welcome-weight').value==='lbs'&&$('welcome-distance').value==='mi','Setup defaults reflect preferences');
    $('welcome-body').value='176.3698';$('welcome-weight').value='kg';$('welcome-weight').dispatchEvent(new Event('change',{bubbles:true}));
    check(Math.abs(+$('welcome-body').value-80)<.01,'Setup weight converts on unit change');
    $('welcome-profile').dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));
    check(JSON.parse(Health.state()).setupComplete===true,'Setup completion persists');
    check(Math.abs(JSON.parse(Health.state()).bodyWeightKg-80)<.01,'Setup saves kg body weight');
    go('insights');check(document.querySelectorAll('.heatmap [data-step-day]').length===30,'Accessible calendar day details');
    close();go('activity');
    check(document.querySelectorAll('[data-step-day]').length===7,'Seven daily detail buttons');
    showStepDay(today());check($('sheet').innerText.includes('walking kcal')&&$('sheet').innerText.includes('mi · estimated'),'Day detail selected units');close();
    state.steps={};const ds=days(7);state.steps[ds[5]]=10000;state.steps[ds[4]]=10000;state.goals={...state.goals,steps:8000};
    check(streak()===2,'Completed-day streak excludes unfinished today');
    state.steps[ds[6]]=10000;check(streak()===3,'Today extends streak');
    state.steps[ds[5]]=0;check(streak()===1,'Missing day breaks streak');
    check(progressCard().includes('versus')===false,'Empty previous week has neutral comparison');
    state=JSON.parse(Health.state());go('journal');
    check(!!$('journal-search'),'Search control');
    journalQuery='unlikely-private-test-match';
    check(rows([{type:'note',value:'alpha',date:today(),id:'sample'}]).includes('No matching records'),'Search excludes unmatched records');
    journalQuery='alpha';check(rows([{type:'note',value:'alpha',date:today(),id:'sample'}]).includes('alpha'),'Search matches record text');
    state.sensor=true;state.permission=false;check(trackingStatus()[0].includes('access'),'Missing permission explanation');
    state.permission=true;state.tracking=false;check(trackingStatus()[0].includes('attention'),'Stopped tracking explanation');
    return {passed:count,version:original.version};
  } finally {
    Health.settings(JSON.stringify({name:original.name||'',stride:original.stride||.7,goals:original.goals,weightUnit:original.weightUnit||'kg',distanceUnit:original.distanceUnit||'km',bodyWeightKg:original.bodyWeightKg||0,setupComplete:!!original.setupComplete}));
    journalQuery='';state=JSON.parse(Health.state());close();go('home');
  }
})()
