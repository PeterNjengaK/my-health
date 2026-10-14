'use strict';
let journalQuery = '';
function profilePrefs(extra = {}) {
  return { name: state.name || '', stride: state.stride || .7, goals: state.goals || {steps:8000,water:2000,sleep:8}, weightUnit: state.weightUnit || 'kg', distanceUnit: state.distanceUnit || 'km', bodyWeightKg: state.bodyWeightKg || 0, ...extra };
}
function savePrefs(extra) {
  const error = Health.settings(JSON.stringify(profilePrefs(extra)));
  if (error) return toast(error);
  state = JSON.parse(Health.state());
  render();
  return true;
}
function unitSelectors(prefix) {
  return `<div class="unit-options"><label for="${prefix}-weight">Weight</label><select id="${prefix}-weight" data-unit="weightUnit"><option value="kg" ${weightUnit()==='kg'?'selected':''}>Kilograms (kg)</option><option value="lbs" ${weightUnit()==='lbs'?'selected':''}>Pounds (lbs)</option></select><label for="${prefix}-distance">Distance</label><select id="${prefix}-distance" data-unit="distanceUnit"><option value="km" ${distanceUnit()==='km'?'selected':''}>Kilometres (km)</option><option value="mi" ${distanceUnit()==='mi'?'selected':''}>Miles (mi)</option></select></div>`;
}
function showSetup() {
  open(`<h2>Make yourself at home.</h2><p>Choose the units you know. No account needed; your name and health records stay on this phone.</p><form id="welcome-profile"><label for="welcome-name">Your name (optional)</label><input id="welcome-name" name="name" maxlength="40" value="${esc(state.name||'')}"><label for="welcome-weight">Weight unit</label><select id="welcome-weight" name="weightUnit"><option value="kg" ${weightUnit()==='kg'?'selected':''}>Kilograms (kg)</option><option value="lbs" ${weightUnit()==='lbs'?'selected':''}>Pounds (lbs)</option></select><label for="welcome-distance">Distance unit</label><select id="welcome-distance" name="distanceUnit"><option value="km" ${distanceUnit()==='km'?'selected':''}>Kilometres (km)</option><option value="mi" ${distanceUnit()==='mi'?'selected':''}>Miles (mi)</option></select><label for="welcome-steps">Daily step goal</label><input id="welcome-steps" name="steps" type="number" min="100" max="100000" value="${state.goals?.steps||8000}" required><label for="welcome-body">Body weight (in chosen weight unit, optional)</label><input id="welcome-body" name="bodyWeight" type="number" min="1" max="1500" step="any" value="${state.bodyWeightKg ? weightValue(state.bodyWeightKg).toFixed(1):''}"><p>Activity access lets Android count your steps automatically. Notifications show tracking and reminders. GPS location is requested only when you start a workout; you choose Health Connect access separately.</p><button class="primary">Save & continue</button></form><button class="secondary" data-polish="later">Use current settings for now</button>`);
}
function trackingStatus() {
  if (!state.sensor) return ['Step sensor unavailable', 'This phone has no hardware step counter. You can import steps from Health Connect on supported phones.'];
  if (!state.permission) return ['Activity access needed', 'Allow activity recognition once to start automatic steps.'];
  if (!state.tracking) return ['Tracking needs attention', 'Android has stopped the tracking service. Reopen the app, check activity access, and review background battery settings.'];
  return ['Automatic steps are running', 'New day at 00:00. Android force-stop or revoked access can interrupt tracking.'];
}
function streak() {
  const dates = days(366).reverse();
  if (step(dates[0]) < (state.goals?.steps||8000)) dates.shift();
  let n=0;
  for (const d of dates) { if (step(d) < (state.goals?.steps||8000)) break; n++; }
  return n;
}
function progressCard() {
  const ds=days(14), current=ds.slice(7).reduce((n,d)=>n+step(d),0), previous=ds.slice(0,7).reduce((n,d)=>n+step(d),0);
  return `<div class="card progress-summary"><div><span class="eyebrow">Goal streak</span><strong>${streak()} <small>days</small></strong><span class="small muted">Consecutive days meeting your current goal</span></div><div><span class="eyebrow">Last 7 days</span><strong>${num(current)} <small>steps</small></strong><span class="small muted">${previous ? `${current>=previous?'+':''}${Math.round((current-previous)/previous*100)}% versus the previous 7 days` : 'A comparison appears as your history builds'}</span></div></div>`;
}
function showStepDay(day) {
  if (!days(proAccess()?90:7).includes(day)) return;
  const n=step(day), goal=state.goals?.steps||8000, kcal=walkingCalories(day);
  open(`<h2>${dateLabel(day,{weekday:'long',month:'short',day:'numeric'})}</h2><p>Your daily movement</p><div class="grid"><div class="card stat"><strong>${num(n)}</strong><span>steps</span></div><div class="card stat"><strong>${distanceValue(n*(state.stride||.7)/1000).toFixed(2)}</strong><span>${distanceUnit()} · estimated</span></div><div class="card stat"><strong>${kcal===null?'—':num(kcal)}</strong><span>walking kcal · estimated</span></div><div class="card stat"><strong>${Math.round(n/goal*100)}%</strong><span>of your current ${num(goal)}-step goal</span></div></div><p>Distance and walking calories use your current stride and available weight. Resting calories and other workouts are excluded.</p>${polishRows((state.entries||[]).filter(e=>e.date===day),false)}<button class="secondary" data-action="close">Done</button>`);
}
const polishHome=home, polishActivity=activity, polishSettings=settings, polishJournal=journal, polishRows=rows;
const polishMonth=monthOverview;
monthOverview=function(){
  return polishMonth().replace(/<div class="heat ([^"]*)" title="(\d{4}-\d{2}-\d{2}): (\d+) steps"><span>(\d+)<\/span><\/div>/g, '<button class="heat $1" data-step-day="$2" aria-label="$2: $3 steps"><span>$4</span></button>');
};
home=function(){
  let html=polishHome();
  // Keep the estimate explanation nearby without overwhelming the daily summary.
  html=html.replace('Daily calories burned · walking estimate','Walking calories today').replace('Estimated from today’s steps, stride and body weight. Excludes resting calories and other workouts; not a measured total.','Estimate from steps and body weight. Excludes resting calories and other workouts.');
  if (!state.setupComplete) html=html.replace('</header>','</header><button class="setup-banner" data-polish="setup">Personalize your name, units & goals →</button>');
  return html.replace('<div class="section-title"><h2>A little check-in',progressCard()+'<div class="section-title"><h2>A little check-in');
};
activity=function(){
  const [title,description]=trackingStatus();
  const ds=days(7).reverse();
  return polishActivity()+`<div class="section-title"><h2>Daily movement details</h2></div><div class="card day-list">${ds.map(d=>`<button data-step-day="${d}"><span>${dateLabel(d)}</span><strong>${num(step(d))} steps →</strong></button>`).join('')}</div><div class="card"><h3>${title}</h3><p class="small muted">${description}</p>${!state.permission&&state.sensor?'<button class="secondary" data-action="tracking">Allow activity access</button>':''}<button class="link" data-action="battery">Review background settings</button></div>`;
};
settings=function(){return polishSettings().replace('<div class="section-title"><h2>Everyday essentials',`<div class="section-title"><h2>Your measurement units</h2></div><div class="card">${unitSelectors('you')}<p class="small muted">Used for measurements, walking distance, GPS and pace. You can mix pounds with kilometres or kilograms with miles.</p><button class="link" data-polish="setup">Personalize your profile</button></div><div class="section-title"><h2>Everyday essentials`);};
rows=function(records,...args){
  if (page==='journal'&&journalQuery) {
    records=records.filter(e=>[e.label,e.note,e.value,e.date,types[e.type]?.name].some(v=>String(v||'').toLocaleLowerCase().includes(journalQuery.toLocaleLowerCase())));
    if(!records.length)return '<div class="empty">No matching records.<br>Try another word or date.</div>';
  }
  return polishRows(records,...args);
};
journal=function(){return polishJournal().replace('<div class="section-title"><h2>Your records',`<label for="journal-search">Search your journal</label><input id="journal-search" type="search" value="${esc(journalQuery)}" placeholder="Meal, measurement, note or date" maxlength="200"><div class="section-title"><h2>Your records`);};
document.addEventListener('input',ev=>{
  if(ev.target.id!=='journal-search')return;
  const cursor=ev.target.selectionStart;
  journalQuery=ev.target.value;render();
  const input=$('journal-search');input.focus();input.setSelectionRange(cursor,cursor);
});
document.addEventListener('change',ev=>{
  if(ev.target.dataset.unit) {savePrefs({[ev.target.dataset.unit]:ev.target.value});toast('Units updated');}
  if(ev.target.id==='welcome-weight') {
    const input=$('welcome-body');
    if(input.value)input.value=(+input.value*(ev.target.value==='lbs'?2.2046226218:1/2.2046226218)).toFixed(2);
  }
});
document.addEventListener('click',ev=>{
  const b=ev.target.closest('button');if(!b)return;
  if(b.dataset.polish==='setup')showSetup();
  if(b.dataset.polish==='later'){savePrefs({setupComplete:true});close();render();}
  if(b.dataset.stepDay)showStepDay(b.dataset.stepDay);
});
document.addEventListener('submit',ev=>{
  if(ev.target.id!=='welcome-profile')return;
  ev.preventDefault();ev.stopImmediatePropagation();
  const o=Object.fromEntries(new FormData(ev.target));
  if(savePrefs({name:o.name.trim(),weightUnit:o.weightUnit,distanceUnit:o.distanceUnit,bodyWeightKg:o.bodyWeight?+o.bodyWeight/(o.weightUnit==='lbs'?2.2046226218:1):0,goals:{...state.goals,steps:+o.steps},setupComplete:true})){close();render();toast('Your profile is ready');}
},true);
document.addEventListener('DOMContentLoaded',()=>{
  // Existing users get an unobtrusive banner; a clean install gets guided setup.
  if(!state.setupComplete&&!state.name&&!(state.entries||[]).length)showSetup();
});
