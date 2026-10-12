'use strict';
const icons = {
  home: '<path d="m3 10 9-7 9 7v10H3z"/><path d="M9 20v-7h6v7"/>',
  activity: '<path d="M3 12h4l3-8 4 16 3-8h4"/>',
  journal:
    '<rect x="5" y="3" width="15" height="18" rx="2"/><path d="M9 8h7M9 12h7M9 16h4M3 7h4M3 12h4M3 17h4"/>',
  insights: '<path d="M4 20V10m8 10V4m8 16v-7"/>',
  settings:
    '<circle cx="12" cy="12" r="4"/><path d="M12 2v3m0 14v3M2 12h3m14 0h3M5 5l2 2m10 10 2 2M5 19l2-2M17 7l2-2"/>',
};
const svg = (name) =>
  `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">${icons[name] || icons.activity}</svg>`;
const types = {
  water: {
    name: 'Water',
    icon: '◉',
    color: '',
    unit: 'ml',
    hint: 'Keep a record of your drinks',
    fields: [['value', 'Amount (ml)', 'number', '250']],
  },
  weight: {
    name: 'Weight',
    icon: '⚖',
    color: 'peach',
    unit: 'kg',
    hint: 'Follow your weight over time',
    fields: [['value', 'Weight (kg)', 'number', '']],
  },
  sleep: {
    name: 'Sleep',
    icon: '☾',
    color: 'purple',
    unit: 'hrs',
    hint: 'Log how long you slept',
    fields: [
      ['value', 'Sleep duration (hours)', 'number', ''],
      ['label', 'Sleep quality', 'select', ['Restful', 'Okay', 'Restless']],
    ],
  },
  workout: {
    name: 'Workout',
    icon: '↗',
    color: 'green',
    unit: 'min',
    hint: 'Record the movement that matters',
    fields: [
      [
        'label',
        'Activity',
        'select',
        ['Walking', 'Running', 'Cycling', 'Strength training', 'Swimming', 'Yoga', 'Other'],
      ],
      ['value', 'Duration (minutes)', 'number', ''],
      ['distance', 'Distance (km, optional)', 'number', ''],
    ],
  },
  food: {
    name: 'Nutrition',
    icon: '♧',
    color: 'peach',
    unit: 'kcal',
    hint: 'Meals, energy and macronutrients',
    fields: [
      ['label', 'Meal / food', 'text', ''],
      ['value', 'Calories (kcal)', 'number', ''],
      ['protein', 'Protein (g, optional)', 'number', ''],
      ['carbs', 'Carbohydrates (g, optional)', 'number', ''],
      ['fat', 'Fat (g, optional)', 'number', ''],
    ],
  },
  bloodpressure: {
    name: 'Blood pressure',
    icon: '♡',
    color: 'peach',
    unit: 'mmHg',
    hint: 'Enter a reading from your monitor',
    fields: [
      ['value', 'Systolic (mmHg)', 'number', ''],
      ['diastolic', 'Diastolic (mmHg)', 'number', ''],
    ],
  },
  heartrate: {
    name: 'Heart rate',
    icon: '♥',
    color: 'peach',
    unit: 'bpm',
    hint: 'Record a measured heart rate',
    fields: [['value', 'Heart rate (bpm)', 'number', '']],
  },
  glucose: {
    name: 'Blood glucose',
    icon: '◇',
    color: '',
    unit: 'mmol/L',
    hint: 'Enter a reading from your glucose meter',
    fields: [
      ['value', 'Blood glucose', 'number', ''],
      ['unit', 'Unit', 'select', ['mmol/L', 'mg/dL']],
      ['label', 'Timing', 'select', ['Fasting', 'Before meal', 'After meal', 'Other']],
    ],
  },
  temperature: {
    name: 'Temperature',
    icon: '♨',
    color: 'peach',
    unit: '°C',
    hint: 'Record a thermometer reading',
    fields: [['value', 'Temperature (°C)', 'number', '']],
  },
  mood: {
    name: 'Mood',
    icon: '☀',
    color: 'green',
    unit: '/ 5',
    hint: 'Make a little room for how you feel',
    fields: [
      [
        'value',
        'How are you feeling?',
        'select',
        ['1 — Very low', '2 — Low', '3 — Okay', '4 — Good', '5 — Great'],
      ],
    ],
  },
  symptom: {
    name: 'Symptoms',
    icon: '✚',
    color: 'purple',
    unit: '',
    hint: 'Track symptoms and their intensity',
    fields: [
      ['label', 'Symptom', 'text', ''],
      ['value', 'Intensity', 'select', ['Mild', 'Moderate', 'Severe']],
    ],
  },
  medication: {
    name: 'Medication',
    icon: '✦',
    color: 'purple',
    unit: '',
    hint: 'Create a daily medication reminder',
    fields: [
      ['label', 'Medication name', 'text', ''],
      ['value', 'Dose (e.g. 1 tablet)', 'text', ''],
      ['time', 'Daily reminder time', 'time', '09:00'],
    ],
  },
  period: {
    name: 'Cycle',
    icon: '❀',
    color: 'peach',
    unit: '',
    hint: 'Record cycle days and symptoms',
    fields: [
      ['value', 'Flow', 'select', ['Spotting', 'Light', 'Medium', 'Heavy']],
      ['label', 'Cycle note', 'text', ''],
    ],
  },
  note: {
    name: 'Health note',
    icon: '≡',
    color: 'green',
    unit: '',
    hint: 'Your personal health journal',
    fields: [
      ['label', 'Title', 'text', ''],
      ['value', 'Note', 'textarea', ''],
    ],
  },
  dose: {
    name: 'Dose taken',
    icon: '✓',
    color: 'purple',
    unit: '',
    hint: 'Medication adherence',
    fields: [],
  },
};
let state = {},
  page = 'home',
  filter = 'all',
  modal = false,
  lastSnapshot = '';
const $ = (id) => document.getElementById(id),
  esc = (v) =>
    String(v ?? '').replace(
      /[&<>"']/g,
      (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c],
    ),
  num = (n) => Math.round(Number(n) || 0).toLocaleString(),
  today = () => state.today || localDate(new Date());
function localDate(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}
function dateLabel(d, opts = { month: 'short', day: 'numeric' }) {
  return new Date(d + 'T12:00:00').toLocaleDateString(undefined, opts);
}
function entries(type, day = today()) {
  return (state.entries || []).filter((e) => e.type === type && (!day || e.date === day));
}
function sum(type, day = today()) {
  return entries(type, day).reduce((s, e) => s + (parseFloat(e.value) || 0), 0);
}
function last(type) {
  return entries(type, null).sort(
    (a, b) => b.date.localeCompare(a.date) || b.created - a.created,
  )[0];
}
function step(day = today()) {
  return state.steps?.[day] || 0;
}
function days(n) {
  return Array.from({ length: n }, (_, i) => {
    let d = new Date(today() + 'T12:00:00');
    d.setDate(d.getDate() - n + 1 + i);
    return localDate(d);
  });
}
function refresh() {
  try {
    let raw = Health.state();
    if (raw === lastSnapshot) return;
    lastSnapshot = raw;
    state = JSON.parse(raw);
    render();
  } catch (e) {
    toast('Unable to load your records');
  }
}
function header(title, kicker) {
  return `<header><div><div class="eyebrow">${kicker}</div><h1>${title}</h1></div><button class="avatar" data-page="settings" aria-label="Profile">${esc((state.name || 'P')[0].toUpperCase())}</button></header>`;
}
function render() {
  if (modal) return;
  let scroll = window.scrollY;
  const views = {
    home: home,
    activity: activity,
    journal: journal,
    insights: insights,
    settings: settings,
  };
  $('app').innerHTML = views[page]();
  $('nav').innerHTML = Object.keys(views)
    .map(
      (p) =>
        `<button data-page="${p}" class="${page === p ? 'active' : ''}">${svg(p)}<span>${{ home: 'Today', activity: 'Activity', journal: 'Journal', insights: 'Insights', settings: 'You' }[p]}</span></button>`,
    )
    .join('');
  window.scrollTo(0, scroll);
}
function go(p) {
  close();
  page = p;
  window.scrollTo(0, 0);
  render();
}
function ring() {
  let n = step(),
    goal = state.goals?.steps || 8000,
    pct = Math.min(n / goal, 1),
    distance = (n * (state.stride || 0.7)) / 1000;
  return `<section class="hero"><div class="hero-top"><div><div class="eyebrow">Your daily movement</div><h2>Every step counts.</h2></div><span class="pill"><i class="dot"></i>${state.tracking && state.permission ? 'Tracking' : 'Paused'}</span></div><div class="ring"><svg viewBox="0 0 226 226"><circle class="track" cx="113" cy="113" r="101"/><circle class="fill" cx="113" cy="113" r="101" stroke-dasharray="635" stroke-dashoffset="${635 * (1 - pct)}"/></svg><div class="ring-value"><div class="shoe">${svg('activity')}</div><strong>${num(n)}</strong><span>of ${num(goal)} steps</span></div></div><div class="hero-foot"><div><strong>${distance.toFixed(2)}</strong><span>km · estimated</span></div><div><strong>${Math.round(pct * 100)}%</strong><span>daily goal</span></div><div><strong>${num(Math.max(0, goal - n))}</strong><span>steps to go</span></div></div></section>`;
}
function metric(type, value, sub, pct) {
  let t = types[type];
  return `<button class="card metric" data-add="${type}"><div class="metric-head"><span class="icon ${t.color}">${t.icon}</span>${t.name}</div><strong>${value}<small> ${t.unit}</small></strong><span class="small muted">${sub}</span>${pct !== undefined ? `<div class="progress"><i style="width:${Math.min(100, pct)}%"></i></div>` : ''}</button>`;
}
function home() {
  let water = sum('water'),
    sleep = sum('sleep'),
    w = last('weight');
  return (
    header(
      `Hello, ${esc(state.name || 'there')}.`,
      dateLabel(today(), { weekday: 'long', month: 'long', day: 'numeric' }),
    ) +
    ring() +
    (!state.tracking || !state.permission
      ? `<div class="notice">Start your daily step count. Allow physical activity access to count steps while your phone is with you.<button data-action="tracking">Enable step tracking</button></div>`
      : '') +
    `<div class="section-title"><h2>Your daily balance</h2><button class="link" data-page="journal">Log health +</button></div><div class="grid">${metric('water', num(water), `of ${num(state.goals?.water || 2000)} ml`, (water / (state.goals?.water || 2000)) * 100)}${metric('sleep', sleep ? sleep.toFixed(1) : '—', `goal ${state.goals?.sleep || 8} hours`, (sleep / (state.goals?.sleep || 8)) * 100)}${metric('workout', num(sum('workout')), 'minutes of movement')}${metric('weight', w ? esc(w.value) : '—', w ? 'last logged ' + dateLabel(w.date) : 'Add your first weigh-in')}</div><div class="section-title"><h2>A little check-in</h2></div><div class="quick"><button data-add="water" data-quick="250">+ 250 ml water</button><button data-add="mood">☀ How I feel</button><button data-add="food">+ Meal</button></div>${medications()}<div class="section-title"><h2>Today’s journal</h2><button class="link" data-page="journal">View all ↗</button></div>${rows(
      (state.entries || [])
        .filter((e) => e.date === today() && e.type !== 'medication')
        .slice()
        .reverse()
        .slice(0, 4),
      false,
    )}<div class="about">MY HEALTH · MADE FOR YOUR EVERYDAY<br>Private by design. No ads. Just you.</div>`
  );
}
function chart(ds, fn) {
  let max = Math.max(state.goals?.steps || 8000, ...ds.map(fn));
  return `<div class="chart">${ds.map((d) => `<div class="bar-slot"><small>${fn(d) ? num(fn(d)) : ''}</small><div class="bar ${d === today() ? 'today' : ''}" style="height:${Math.max(2, (fn(d) / max) * 105)}px"></div><span>${dateLabel(d, { weekday: 'narrow' })}</span></div>`).join('')}</div>`;
}
function activity() {
  let ds = days(7),
    total = ds.reduce((s, d) => s + step(d), 0),
    active = ds.filter((d) => step(d) > 0).length;
  return (
    header('Keep moving.', 'Activity & progress') +
    ring() +
    `<div class="section-title"><h2>Your week in steps</h2><span class="small muted">Last 7 days</span></div><div class="card">${chart(ds, step)}<div class="stats"><div class="stat"><strong>${num(total)}</strong><span>steps this week</span></div><div class="stat"><strong>${num(active ? total / active : 0)}</strong><span>average on tracked days</span></div></div></div><div class="section-title"><h2>Workouts</h2><button class="link" data-add="workout">Add workout +</button></div>${rows(entries('workout', null).slice().reverse().slice(0, 15))}<div class="notice">Distance is estimated from your step count and stride length. Adjust your stride in You. Tracking starts when enabled; past steps cannot be recovered.<br>Carry your phone when walking. If tracking stops, check the app’s background battery settings.</div>`
  );
}
function journal() {
  let a = (state.entries || [])
    .filter((e) => filter === 'all' || e.type === filter)
    .slice()
    .sort((a, b) => b.date.localeCompare(a.date) || b.created - a.created);
  return (
    header('Your health, recorded.', 'Personal journal') +
    `<p class="page-intro">Small check-ins. A clearer picture over time.</p><div class="catalog">${Object.entries(
      types,
    )
      .filter(([k]) => k !== 'dose')
      .map(
        ([k, t]) =>
          `<button class="card" data-add="${k}"><span class="icon ${t.color}">${t.icon}</span><h3>${t.name}</h3><span class="small">${t.hint}</span></button>`,
      )
      .join(
        '',
      )}</div><div class="section-title"><h2>Your records</h2><span class="small muted">${a.length} entries</span></div><div class="filter"><button data-filter="all" class="${filter === 'all' ? 'active' : ''}">All</button>${Object.entries(
      types,
    )
      .map(
        ([k, t]) =>
          `<button data-filter="${k}" class="${filter === k ? 'active' : ''}">${t.name}</button>`,
      )
      .join('')}</div>${rows(a, true, true)}`
  );
}
function rows(a, canDelete = true, dates = false) {
  if (!a.length)
    return '<div class="empty">A fresh start.<br>Your records will appear here when you add them.</div>';
  let previous = '';
  return a
    .map((e) => {
      let t = types[e.type] || types.note,
        head = '';
      if (dates && e.date !== previous) {
        head = `<div class="journal-date">${dateLabel(e.date, { weekday: 'long', month: 'short', day: 'numeric' })}</div>`;
        previous = e.date;
      }
      return (
        head +
        `<div class="row"><span class="icon ${t.color}">${t.icon}</span><div class="grow"><strong>${esc(e.label || t.name)}</strong><span>${esc(e.note || dateLabel(e.date))}${e.type === 'food' ? ` · P ${esc(e.protein || 0)} / C ${esc(e.carbs || 0)} / F ${esc(e.fat || 0)} g` : ''}</span></div><div class="value">${esc(e.value)}${e.diastolic ? '/' + esc(e.diastolic) : ''}<br><span class="muted small">${esc(e.unit || t.unit)}</span></div>${canDelete ? `<button class="delete" data-delete="${e.id}" aria-label="Delete entry">×</button>` : ''}</div>`
      );
    })
    .join('');
}
function medications() {
  let meds = entries('medication', null);
  if (!meds.length) return '';
  return `<div class="section-title"><h2>Medication routine</h2><button class="link" data-add="medication">Add +</button></div><div class="card">${meds
    .map((e) => {
      let done = entries('dose').some((x) => x.medId === e.id);
      return `<div class="row"><span class="icon purple">✦</span><div class="grow"><strong>${esc(e.label)}</strong><span>${esc(e.value)} · Daily ${esc(e.time)}</span></div><button class="check" ${done ? 'disabled' : ''} data-dose="${e.id}">${done ? '✓ Taken' : 'Mark taken'}</button></div>`;
    })
    .join('')}</div>`;
}
function trend(type) {
  let a = entries(type, null)
      .sort((a, b) => a.date.localeCompare(b.date) || a.created - b.created)
      .slice(-14),
    t = types[type];
  if (!a.length) return '';
  let values = a.map((e) => parseFloat(e.value) || 0),
    lo = Math.min(...values),
    hi = Math.max(...values),
    points = values
      .map(
        (v, i) =>
          `${10 + (i * 280) / Math.max(1, values.length - 1)},${85 - ((v - lo) / Math.max(0.1, hi - lo)) * 65}`,
      )
      .join(' ');
  return `<div class="section-title"><h2>${t.name} trend</h2><button class="link" data-add="${type}">Add +</button></div><div class="card"><div class="flex"><div><div class="eyebrow">Latest entry</div><span class="summary-number">${esc(a[a.length - 1].value)}</span> <span class="muted">${t.unit}</span></div><span class="badge">${a.length} readings</span></div><svg class="sparkline" viewBox="0 0 300 100" preserveAspectRatio="none"><path d="M10 85H290" stroke="#e5e9e1"/><polyline points="${points}" fill="none" stroke="#164c3d" stroke-width="2.5" stroke-linejoin="round"/>${values.map((v, i) => `<circle cx="${10 + (i * 280) / Math.max(1, values.length - 1)}" cy="${85 - ((v - lo) / Math.max(0.1, hi - lo)) * 65}" r="3" fill="#164c3d"/>`).join('')}</svg><div class="flex small muted"><span>${dateLabel(a[0].date)}</span><span>${dateLabel(a[a.length - 1].date)}</span></div></div>`;
}
function insights() {
  let ds = days(7),
    goal = state.goals?.steps || 8000,
    waterDays = ds.filter((d) => entries('water', d).length),
    sleepDays = ds.filter((d) => entries('sleep', d).length),
    foodToday = entries('food');
  return (
    header('See the bigger picture.', 'Your insights') +
    `<div class="card"><div class="flex"><h2>Weekly movement</h2><span class="badge">${ds.filter((d) => step(d) >= goal).length} goals met</span></div>${chart(ds, step)}<p class="small muted">${num(ds.reduce((s, d) => s + step(d), 0))} steps recorded over the last seven days.</p></div><div class="section-title"><h2>Habit averages</h2><span class="small muted">Logged days · 7 days</span></div><div class="grid"><div class="card stat"><span class="icon">◉</span><strong>${waterDays.length ? num(waterDays.reduce((s, d) => s + sum('water', d), 0) / waterDays.length) : '—'}</strong><span>ml water / logged day</span></div><div class="card stat"><span class="icon purple">☾</span><strong>${sleepDays.length ? (sleepDays.reduce((s, d) => s + sum('sleep', d), 0) / sleepDays.length).toFixed(1) : '—'}</strong><span>hours sleep / logged day</span></div></div><div class="section-title"><h2>Today’s nutrition</h2><button class="link" data-add="food">Log meal +</button></div><div class="card"><span class="summary-number">${num(sum('food'))}</span> <span class="muted">kcal logged</span><div class="hero-foot"><div><strong>${num(foodToday.reduce((s, e) => s + (+e.protein || 0), 0))} g</strong><span class="muted">protein</span></div><div><strong>${num(foodToday.reduce((s, e) => s + (+e.carbs || 0), 0))} g</strong><span class="muted">carbs</span></div><div><strong>${num(foodToday.reduce((s, e) => s + (+e.fat || 0), 0))} g</strong><span class="muted">fat</span></div></div></div>${['weight', 'heartrate', 'sleep', 'mood'].map(trend).join('')}<div class="section-title"><h2>Recent measurements</h2></div>${rows(
      (state.entries || [])
        .filter((e) => ['bloodpressure', 'glucose', 'temperature'].includes(e.type))
        .slice()
        .reverse()
        .slice(0, 8),
      false,
    )}<div class="notice">These charts summarize your records. They do not diagnose conditions or replace professional care. Blood pressure, glucose, heart rate and temperature need a separate measuring device.</div>`
  );
}
function settings() {
  return (
    header('Make it yours.', 'Profile & preferences') +
    `<div class="card"><div class="settings-row"><div><strong>Daily goals & profile</strong><span>${num(state.goals?.steps || 8000)} steps · ${num(state.goals?.water || 2000)} ml · ${state.goals?.sleep || 8} hrs</span></div><button class="switch" data-action="goals">Edit</button></div><div class="settings-row"><div><strong>Automatic step tracking</strong><span>${state.sensor ? 'Hardware step counter available' : 'No hardware step counter available'}<br>${state.permission ? 'Activity permission allowed' : 'Activity permission required'}</span></div><button class="switch" data-action="tracking">${state.tracking ? 'Pause' : 'Enable'}</button></div><div class="settings-row"><div><strong>Reminder notifications</strong><span>${state.notifications ? 'Notifications enabled' : 'Allow notifications for medication reminders'}</span></div><button class="switch" data-action="notifications">Settings</button></div><div class="settings-row"><div><strong>Background activity</strong><span>Allow background use if your phone pauses tracking.</span></div><button class="switch" data-action="battery">Manage</button></div></div><div class="section-title"><h2>Your data belongs to you</h2></div><div class="card"><h3>Local storage. No account.</h3><p class="small muted">Health records stay on this phone. No advertising, analytics, cloud uploads or network access. Export a backup before uninstalling or changing phones.</p><button class="secondary" data-action="export">Export JSON backup</button><button class="secondary" data-action="restore">Restore from backup</button></div><div class="notice">Medication reminders repeat daily until the medication entry is deleted. Android may delay reminders to save battery; this app does not request exact alarm access.<br><br>Steps begin from the first sensor reading after tracking is enabled. After a long interruption spanning midnight, the sensor’s accumulated steps may be assigned to the day it reports them.</div><div class="about">MY HEALTH 1.0<br>Built for Peter. A calmer way to care for yourself.</div>`
  );
}
function open(content) {
  modal = true;
  $('sheet').innerHTML = content;
  $('overlay').hidden = false;
}
function close() {
  modal = false;
  $('overlay').hidden = true;
  render();
}
function field(f) {
  let [key, label, type, val] = f;
  return `<label for="${key}">${label}</label>${type === 'select' ? `<select id="${key}" name="${key}">${val.map((v) => `<option>${esc(v)}</option>`).join('')}</select>` : type === 'textarea' ? `<textarea id="${key}" name="${key}" required maxlength="1000"></textarea>` : `<input id="${key}" name="${key}" type="${type}" value="${esc(val)}" ${type === 'number' ? 'min="0.1" max="100000" step="any" inputmode="decimal"' : ''} ${label.includes('optional') || (key === 'label' && label === 'Cycle note') ? '' : 'required'} maxlength="200">`}`;
}
function add(type) {
  let t = types[type];
  open(
    `<h2>${t.name}</h2><p>${t.hint}. ${type === 'medication' ? 'Repeats daily; delete the entry to stop reminders.' : ''}</p><form id="entry" data-type="${type}">${t.fields.map(field).join('')}<label for="date">Date</label><input id="date" name="date" type="date" value="${today()}" max="${today()}" required><label for="note">Notes (optional)</label><textarea id="note" name="note" maxlength="4000" placeholder="Anything you’d like to remember"></textarea><button class="primary" type="submit">Save ${t.name.toLowerCase()}</button><button class="secondary" type="button" data-action="close">Cancel</button></form>`,
  );
}
function goals() {
  open(
    `<h2>Your everyday goals</h2><p>Choose targets that work for you.</p><form id="goals"><label>Your name</label><input name="name" value="${esc(state.name || 'Peter')}" maxlength="40" required><label>Daily step goal</label><input name="steps" type="number" min="100" max="100000" value="${state.goals?.steps || 8000}" required><label>Daily water goal (ml)</label><input name="water" type="number" min="100" max="10000" value="${state.goals?.water || 2000}" required><label>Sleep goal (hours)</label><input name="sleep" type="number" min="1" max="24" step="0.5" value="${state.goals?.sleep || 8}" required><label>Stride length (metres)</label><input name="stride" type="number" min="0.2" max="2" step="0.01" value="${state.stride || 0.7}" required><p>Stride length is the distance of one step. It is used only to estimate walking distance.</p><button class="primary">Save preferences</button><button class="secondary" type="button" data-action="close">Cancel</button></form>`,
  );
}
function save(e) {
  let error = Health.add(JSON.stringify(e));
  if (error) {
    toast(error);
    return false;
  }
  state = JSON.parse(Health.state());
  close();
  toast('Saved to your journal');
  return true;
}
document.addEventListener('submit', (ev) => {
  ev.preventDefault();
  let form = ev.target,
    o = Object.fromEntries(new FormData(form));
  if (form.id === 'entry') {
    o.type = form.dataset.type;
    if (o.type === 'mood') o.value = o.value.split(' ')[0];
    if (o.type === 'sleep' && +o.value > 24) {
      toast('Sleep duration must be 24 hours or less');
      return;
    }
    if (o.type === 'medication') Health.notifications();
    save(o);
  } else if (form.id === 'goals') {
    let error = Health.settings(
      JSON.stringify({
        name: o.name.trim(),
        stride: +o.stride,
        goals: { steps: +o.steps, water: +o.water, sleep: +o.sleep },
      }),
    );
    if (error) return toast(error);
    state = JSON.parse(Health.state());
    close();
    toast('Preferences saved');
  }
});
document.addEventListener('click', (ev) => {
  let b = ev.target.closest('button');
  if (!b) return;
  if (b.dataset.page) return go(b.dataset.page);
  if (b.dataset.add) {
    if (b.dataset.quick) return save({ type: 'water', value: b.dataset.quick, date: today() });
    return add(b.dataset.add);
  }
  if (b.dataset.filter) {
    filter = b.dataset.filter;
    render();
    return;
  }
  if (b.dataset.delete) {
    open(
      `<h2>Delete this entry?</h2><p>This record will be removed from your journal.${(state.entries || []).find((e) => e.id === b.dataset.delete)?.type === 'medication' ? ' Its daily reminder will also stop.' : ''}</p><button class="primary danger" data-confirm-delete="${b.dataset.delete}">Delete entry</button><button class="secondary" data-action="close">Keep entry</button>`,
    );
    return;
  }
  if (b.dataset.confirmDelete) {
    Health.delete(b.dataset.confirmDelete);
    state = JSON.parse(Health.state());
    close();
    toast('Entry deleted');
    return;
  }
  if (b.dataset.dose) {
    let med = (state.entries || []).find((e) => e.id === b.dataset.dose);
    return save({ type: 'dose', medId: med.id, label: med.label, value: med.value, date: today() });
  }
  let act = b.dataset.action;
  if (act === 'close') close();
  else if (act === 'goals') goals();
  else if (act === 'tracking') {
    if (!state.sensor) {
      toast('This phone does not have a step counter sensor');
      return;
    }
    Health.tracking(!state.tracking);
    setTimeout(refresh, 700);
  } else if (['notifications', 'battery', 'export', 'restore'].includes(act)) Health[act]();
});
$('overlay').addEventListener('click', (e) => {
  if (e.target === $('overlay')) close();
});
function back() {
  if (modal) close();
  else go('home');
}
function toast(s) {
  $('toast').textContent = s;
  $('toast').style.display = 'block';
  clearTimeout(window.toastTimer);
  window.toastTimer = setTimeout(() => ($('toast').style.display = 'none'), 3000);
}
refresh();
setInterval(refresh, 3000);
document.addEventListener('visibilitychange', () => {
  if (!document.hidden) refresh();
});
