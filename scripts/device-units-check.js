(() => {
  const original = JSON.parse(Health.state());
  const ids = [];
  const check = (ok, message) => { if (!ok) throw new Error(message); };
  try {
    const prefs = { name: original.name || '', stride: original.stride || .7, goals: original.goals, weightUnit: 'lbs', distanceUnit: 'mi', bodyWeightKg: 80 };
    check(!Health.settings(JSON.stringify(prefs)), 'Save units');
    state = JSON.parse(Health.state());
    check(state.weightUnit === 'lbs' && state.distanceUnit === 'mi', 'Persist units');
    check(Math.abs(weightValue(80) - 176.3698) < .001, 'kg to pounds');
    check(Math.abs(distanceValue(1.609344) - 1) < 1e-9, 'km to miles');
    const sample = { type: 'weight', value: 176.369809744, date: today() };
    normalizeEntryUnits(sample);
    check(Math.abs(sample.value - 80) < .00001 && sample.unit === 'kg', 'Store canonical weight');
    check(!Health.add(JSON.stringify(sample)), 'Save converted record');
    const updated = JSON.parse(Health.state());
    ids.push(...updated.entries.filter(e => !original.entries.some(o => o.id === e.id)).map(e => e.id));
    state = updated;
    check(rows([state.entries.find(e => e.id === ids[0])]).includes('176.4'), 'Display pounds');
    check(home().includes('Walking calories today'), 'Daily calorie card');
    check(home().includes('mi · estimated'), 'Home distance unit');
    check(activity().includes('estimated mi'), 'Weekly distance unit');
    add('weight');
    check(document.querySelector('label[for=value]').textContent.includes('lbs'), 'Weight form label');
    close();
    goals();
    check(document.querySelector('[name=weightUnit]').value === 'lbs', 'Profile selection');
    check(document.querySelector('[name=distanceUnit]').value === 'mi', 'Distance selection');
    close();
    return { passed: 12, version: state.version };
  } finally {
    for (const id of ids) Health.delete(id);
    Health.settings(JSON.stringify({ name: original.name || '', stride: original.stride || .7, goals: original.goals, weightUnit: original.weightUnit || 'kg', distanceUnit: original.distanceUnit || 'km', bodyWeightKg: original.bodyWeightKg || 0 }));
    state = JSON.parse(Health.state());
    close(); go('home');
  }
})()
