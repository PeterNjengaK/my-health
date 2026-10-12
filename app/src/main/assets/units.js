'use strict';
function weightUnit() { return state.weightUnit === 'lbs' ? 'lbs' : 'kg'; }
function distanceUnit() { return state.distanceUnit === 'mi' ? 'mi' : 'km'; }
function weightValue(kg) { return kg * (weightUnit() === 'lbs' ? 2.2046226218 : 1); }
function distanceValue(km) { return km / (distanceUnit() === 'mi' ? 1.609344 : 1); }
function weightKg(e) { return +e.value / (e.unit === 'lbs' || e.unit === 'lb' ? 2.2046226218 : 1); }
function normalizeEntryUnits(e) {
  if (e._canonical) return;
  if (e.type === 'weight') { e.value = +e.value / (weightUnit() === 'lbs' ? 2.2046226218 : 1); e.unit = 'kg'; }
  if (e.type === 'workout' && e.distance) e.distance = +e.distance * (distanceUnit() === 'mi' ? 1.609344 : 1);
  e._canonical = true;
}
function walkingCalories(day = today()) {
  const weights = entries('weight', null).filter(e => e.date <= day).sort((a,b) => b.date.localeCompare(a.date) || b.created - a.created);
  const kg = weights.length ? weightKg(weights[0]) : +state.bodyWeightKg;
  return kg > 0 ? Math.round(step(day) * (state.stride || 0.7) / 1000 * kg * 0.5) : null;
}
const unitsHome = home;
home = function() {
  types.weight.unit = weightUnit();
  const kcal = walkingCalories();
  const card = `<div class="card"><div class="eyebrow">Daily calories burned · walking estimate</div><span class="summary-number">${kcal === null ? '—' : num(kcal)}</span> <span class="muted">kcal</span><p class="small muted">${kcal === null ? 'Add your body weight in Daily goals & profile, or log a weigh-in.' : 'Estimated from today’s steps, stride and body weight. Excludes resting calories and other workouts; not a measured total.'}</p><button class="link" data-action="goals">Weight & unit preferences</button></div>`;
  return unitsHome().replace('<div class="section-title"><h2>Your daily balance', card + '<div class="section-title"><h2>Your daily balance');
};
document.addEventListener('change', ev => {
  if (ev.target.id !== 'profile-weight-unit') return;
  const input = document.querySelector('[name="bodyWeight"]');
  if (input.value) input.value = (+input.value * (ev.target.value === 'lbs' ? 2.2046226218 : 1 / 2.2046226218)).toFixed(1);
});
