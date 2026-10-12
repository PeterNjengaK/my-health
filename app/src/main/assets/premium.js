'use strict';

// Keep the original journal and add focused screens for the v2 capabilities.
const baseHome = home,
  baseActivity = activity,
  baseInsights = insights;
const baseAdd = add,
  baseRender = render,
  baseRows = rows;
let reportDays = 30,
  currentProduct = null,
  snapshotAt = Date.now();

function applyTheme() {
  const mode = state.theme || 'system';
  const dark =
    mode === 'dark' ||
    (mode === 'system' && (state.systemDark ?? matchMedia('(prefers-color-scheme: dark)').matches));
  document.documentElement.dataset.theme = dark ? 'dark' : 'light';
}

render = function () {
  snapshotAt = Date.now();
  applyTheme();
  baseRender();
};
sum = function (type, day = today()) {
  // The same sleep/workout can be recorded by a phone and a wearable.
  // Compare totals by source rather than summing overlapping sources.
  const totals = {};
  for (const e of entries(type, day)) {
    const source = e.source === 'healthConnect' ? e.origin || 'healthConnect' : 'local';
    totals[source] = (totals[source] || 0) + (parseFloat(e.value) || 0);
  }
  return Math.max(0, ...Object.values(totals));
};

function routePreview(route, big = false) {
  if (!route?.length)
    return `<div class="route-empty ${big ? 'big' : ''}">${svg('activity')}<strong>Your route starts here</strong><span>Head outdoors for a clear GPS signal.</span></div>`;
  const coordinates = route.filter((p) => Number.isFinite(+p.lat) && Number.isFinite(+p.lon));
  if (!coordinates.length) return '';
  const meanLat = coordinates.reduce((n, p) => n + +p.lat, 0) / coordinates.length;
  const cos = Math.cos((meanLat * Math.PI) / 180);
  const xs = coordinates.map((p) => +p.lon * cos),
    ys = coordinates.map((p) => +p.lat);
  const minX = Math.min(...xs),
    minY = Math.min(...ys);
  const spanX = Math.max(Math.max(...xs) - minX, 0.00015),
    spanY = Math.max(Math.max(...ys) - minY, 0.00015),
    scale = Math.min(260 / spanX, 145 / spanY),
    offsetX = (320 - spanX * scale) / 2,
    offsetY = (210 - spanY * scale) / 2;
  const point = (i) =>
    `${offsetX + (xs[i] - minX) * scale},${210 - offsetY - (ys[i] - minY) * scale}`;
  const segments = [];
  coordinates.forEach((p, i) => {
    if (!i || p.segment !== coordinates[i - 1].segment) segments.push([]);
    segments.at(-1).push(point(i));
  });
  const first = point(0).split(','),
    end = point(coordinates.length - 1).split(',');
  return `<div class="route-map"><svg viewBox="0 0 320 210" role="img" aria-label="Recorded GPS route"><defs><pattern id="grid" width="20" height="20" patternUnits="userSpaceOnUse"><path d="M20 0H0V20" fill="none" stroke="currentColor" stroke-opacity=".09"/></pattern></defs><rect width="320" height="210" fill="url(#grid)"/>${segments.map((s) => `<polyline points="${s.join(' ')}" fill="none" stroke="#72ad8a" stroke-width="4" stroke-linejoin="round" stroke-linecap="round"/>`).join('')}<circle cx="${first[0]}" cy="${first[1]}" r="5" fill="#164c3d"/><circle cx="${end[0]}" cy="${end[1]}" r="6" fill="#c7ee8f" stroke="#164c3d" stroke-width="2"/></svg><span class="map-label">GPS route · offline preview</span></div>`;
}

function timeFormat(ms) {
  const seconds = Math.floor((ms || 0) / 1000);
  return `${Math.floor(seconds / 3600) ? Math.floor(seconds / 3600) + ':' : ''}${String(Math.floor(seconds / 60) % 60).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;
}

function gpsCard() {
  const w = state.workout || {};
  if (!w.active)
    return `<section class="workout-intro card"><div class="flex"><span class="icon green">${svg('activity')}</span><span class="badge">GPS WORKOUTS</span></div><h2>A little further, today.</h2><p class="muted small">Record your route, distance and pace. Your phone keeps tracking when the screen is off.</p><div class="workout-types"><button data-start="Walking">Walk ↗</button><button data-start="Running">Run ↗</button><button data-start="Cycling">Ride ↗</button></div></section>`;
  const km = distanceValue((w.distance || 0) / 1000),
    minutes = (w.elapsed || 0) / 60000;
  const pace = km > 0.01 ? minutes / km : null;
  const paceSeconds = pace ? Math.round(pace * 60) : 0;
  const paceText = pace
    ? `${Math.floor(paceSeconds / 60)}:${String(paceSeconds % 60).padStart(2, '0')}`
    : '—';
  return `<section class="card live-workout"><div class="flex"><div><div class="eyebrow">${w.paused ? 'Paused' : 'Live session'}</div><h2>${esc(w.label || 'Workout')}</h2></div><span class="pill live"><i class="dot"></i>${w.paused ? 'Paused' : 'Recording'}</span></div>${routePreview(w.route, true)}<div class="workout-stats"><div><strong>${km.toFixed(2)}</strong><span>${distanceUnit()}</span></div><div><strong id="workout-clock">${timeFormat(w.elapsed)}</strong><span>duration</span></div><div><strong>${paceText}</strong><span>min / ${distanceUnit()}</span></div></div><p class="small muted">${esc(state.gpsStatus || 'Waiting for a GPS fix — best outdoors')}</p><div class="split-buttons"><button class="secondary" data-workout="${w.paused ? 'resume' : 'pause'}">${w.paused ? 'Resume' : 'Pause'}</button><button class="primary" data-workout="finish">Finish & save</button></div></section>`;
}

home = function () {
  let html = baseHome();
  const shortcut = `<div class="feature-strip"><button data-page="activity">${svg('activity')}<span>GPS workout</span></button><button data-action="scan-food">${svg('journal')}<span>Scan a meal</span></button><button data-action="widget">${svg('home')}<span>Step widget</span></button></div>`;
  html = html.replace(
    '<div class="section-title"><h2>Your daily balance',
    shortcut + '<div class="section-title"><h2>Your daily balance',
  );
  if (state.workout?.active)
    html = html.replace(
      '<div class="section-title"><h2>A little check-in',
      `<button class="session-banner" data-page="activity"><i class="dot"></i>${esc(state.workout.label)} ${state.workout.paused ? 'paused' : 'in progress'} · ${timeFormat(state.workout.elapsed)} →</button><div class="section-title"><h2>A little check-in`,
    );
  return html;
};

activity = function () {
  const ds = days(7),
    total = ds.reduce((s, d) => s + step(d), 0);
  return (
    header('Find your stride.', 'Movement, made personal') +
    gpsCard() +
    `<div class="section-title"><h2>Your week in steps</h2><span class="small muted">Last 7 days</span></div><div class="card">${chart(ds, step)}<div class="stats"><div class="stat"><strong>${num(total)}</strong><span>steps this week</span></div><div class="stat"><strong>${distanceValue((total * (state.stride || 0.7)) / 1000).toFixed(1)}</strong><span>estimated ${distanceUnit()}</span></div></div></div><div class="section-title"><h2>Workout history</h2><button class="link" data-add="workout">Log manually +</button></div>${rows(
      entries('workout', null)
        .slice()
        .sort((a, b) => b.created - a.created)
        .slice(0, 20),
    )}<div class="notice">GPS measures your outdoor route; daily step distance uses your stride estimate. Location is collected only during a workout you start. Routes stay on this phone.</div>`
  );
};

rows = function (records, canDelete = true, dates = false) {
  let html = baseRows(records, canDelete, dates);
  if (!canDelete) return html;
  for (const e of records) {
    const marker = `<button class="delete" data-delete="${e.id}"`;
    const actions = `<button class="edit-entry" data-edit="${esc(e.id)}" aria-label="Edit ${esc(types[e.type]?.name || 'entry')}">✎</button>${e.route?.length ? `<button class="edit-entry" data-route="${esc(e.id)}" aria-label="View workout route">↗</button>` : ''}`;
    html = html.replace(marker, actions + marker);
  }
  return html;
};

function monthOverview() {
  const ds = days(reportDays),
    goal = state.goals?.steps || 8000;
  const total = ds.reduce((n, d) => n + step(d), 0);
  const reached = ds.filter((d) => step(d) >= goal).length;
  const weights = entries('weight', null)
    .filter((e) => e.date >= ds[0])
    .sort((a, b) => a.date.localeCompare(b.date) || a.created - b.created);
  const change = weights.length > 1 ? weightValue(weightKg(weights.at(-1)) - weightKg(weights[0])).toFixed(1) : null;
  return `<div class="section-title"><h2>Your ${reportDays}-day picture</h2><span class="badge">${reached} goals met</span></div><div class="filter"><button data-range="7" class="${reportDays === 7 ? 'active' : ''}">7 days</button><button data-range="30" class="${reportDays === 30 ? 'active' : ''}">30 days</button><button data-range="90" class="${reportDays === 90 ? 'active' : ''}">90 days</button></div><div class="grid"><div class="card stat"><span class="eyebrow">Total steps</span><strong>${num(total)}</strong><span>${num(total / reportDays)} per calendar day</span></div><div class="card stat"><span class="eyebrow">Weight change</span><strong>${change === null ? '—' : (+change > 0 ? '+' : '') + change}<small> ${weightUnit()}</small></strong><span>${weights.length} logged measurements</span></div></div><div class="card calendar-card"><div class="eyebrow">Movement calendar</div><div class="heatmap">${ds.map((d) => `<div class="heat ${step(d) >= goal ? 'full' : step(d) > 0 ? 'some' : ''}" title="${d}: ${step(d)} steps"><span>${new Date(d + 'T12:00:00').getDate()}</span></div>`).join('')}</div><p class="small muted">Light: activity recorded · Dark: daily goal reached</p></div><div class="split-buttons"><button class="secondary" data-report="csv">Export all records · CSV</button><button class="secondary" data-report="pdf">30-day report · PDF</button></div>`;
}

insights = function () {
  const html = baseInsights();
  return html.replace(
    '<div class="card">',
    monthOverview() + '<div class="section-title"><h2>Weekly overview</h2></div><div class="card">',
  );
};

settings = function () {
  const setting = (title, description, control) =>
    `<div class="settings-row"><div><strong>${title}</strong><span>${description}</span></div>${control}</div>`;
  return (
    header('Your kind of healthy.', 'You & your preferences') +
    `<div class="profile-card card"><div class="profile-initial">${esc((state.name || '?')[0].toUpperCase())}</div><div><h2>${esc(state.name || 'Your profile')}</h2><span class="small muted">A calmer way to care for yourself</span></div></div><div class="section-title"><h2>Everyday essentials</h2></div><div class="card">${setting('Daily goals & profile', `${num(state.goals?.steps)} steps · ${num(state.goals?.water)} ml · ${state.goals?.sleep} hrs`, '<button class="switch" data-action="goals">Edit</button>')}${setting('Appearance', 'Choose light, dark, or follow your phone.', `<select class="compact-select" id="theme-mode" aria-label="Appearance"><option value="system" ${state.theme === 'system' ? 'selected' : ''}>System</option><option value="light" ${state.theme === 'light' ? 'selected' : ''}>Light</option><option value="dark" ${state.theme === 'dark' ? 'selected' : ''}>Dark</option></select>`)}${setting('Automatic daily steps', 'Runs in the background · new day at 00:00. History stays saved. Manage activity access in Android permissions.', state.permission ? '<span class="badge">AUTOMATIC</span>' : '<button class="switch" data-action="tracking">Allow access</button>')}${setting('Home-screen widget', 'Daily steps and progress at a glance.', '<button class="switch" data-action="widget">Add</button>')}${setting('Background activity', 'Keep steps recording while your phone is asleep.', '<button class="switch" data-action="battery">Manage</button>')}</div><div class="section-title"><h2>Gentle reminders</h2></div><div class="card">${setting('Hydration reminders', `Every ${state.hydrationMinutes || 120} minutes, 8 am–9 pm. Stops for the day once your goal is met.`, `<button class="switch" data-action="hydration">${state.hydration ? 'On' : 'Off'}</button>`)}${setting('Notifications', state.notifications ? 'Notifications enabled' : 'Allow reminders and progress notifications.', '<button class="switch" data-action="notifications">Settings</button>')}</div><div class="section-title"><h2>Connected health</h2></div><div class="card"><div class="flex"><span class="icon green">♡</span><span class="badge">READ-ONLY</span></div><h3 class="connected-title">Health Connect</h3><p class="small muted">Bring in steps, sleep, workouts and measurements from compatible apps and wearables. Import the last 30 days; refresh automatically when you return, up to every 15 minutes.</p><p class="sync-status">${esc(state.syncStatus)}</p><div class="split-buttons"><button class="secondary" data-action="health-permissions">Choose access</button><button class="primary" data-action="health-sync">Sync now</button></div>${!state.healthConnect ? '<p class="small muted">Requires Android 14 or later.</p>' : ''}</div><div class="section-title"><h2>Privacy & your data</h2></div><div class="card">${setting('Fingerprint & screen-lock protection', 'Lock the journal when you leave the app. The widget hides counts while protection is on.', `<button class="switch" data-action="app-lock">${state.appLock ? 'On' : 'Enable'}</button>`)}<p class="small muted">Your health records and GPS routes stay on this phone. Only a scanned product barcode is sent to Open Food Facts for nutrition lookup. No ads or analytics. Pro payments are handled by Google Play.</p><button class="secondary" data-action="privacy">Privacy & health information</button><button class="secondary" data-action="export">Export JSON backup</button><button class="secondary" data-action="restore">Restore backup</button><div class="split-buttons"><button class="secondary" data-report="csv">CSV records</button><button class="secondary" data-report="pdf">PDF report</button></div></div><button class="secondary danger" data-action="delete-health-data">Delete all health data</button><button class="secondary" data-action="pro">My Health Pro</button><div class="about">MY HEALTH ${esc(state.version || '2.1')}<br>Made for your everyday. Built to stay yours.</div>`
  );
};

add = function (type) {
  types.weight.unit = weightUnit();
  types.weight.fields[0][1] = `Weight (${weightUnit()})`;
  types.workout.fields.find(f => f[0] === "distance")[1] = `Distance (${distanceUnit()}, optional)`;
  baseAdd(type);
  if (type === 'food')
    $('entry').insertAdjacentHTML(
      'afterbegin',
      `<button class="scan-button" type="button" data-action="scan-food">Scan a barcode ↗</button><p class="attribution">Nutrition lookup by Open Food Facts</p>`,
    );
  for (const input of document.querySelectorAll('input[type=number]'))
    if (['protein', 'carbs', 'fat'].includes(input.name)) input.min = 0;
};

function editEntry(id) {
  const e = state.entries.find((x) => x.id === id);
  if (!e) return toast('This record no longer exists');
  if (e.type === 'dose') {
    open(
      `<h2>Edit dose record</h2><form id="edit-dose" data-edit="${esc(id)}"><label>Medication</label><input name="label" value="${esc(e.label)}" required><label>Dose</label><input name="value" value="${esc(e.value)}" required><label>Date</label><input type="date" name="date" value="${e.date}" required><button class="primary">Save changes</button></form>`,
    );
    return;
  }
  add(e.type);
  $('sheet').querySelector('h2').textContent = `Edit ${types[e.type].name.toLowerCase()}`;
  const form = $('entry');
  form.dataset.edit = id;
  for (const element of form.elements) {
    if (!element.name || e[element.name] === undefined) continue;
    let value = String(e[element.name]);
    if (e.type === "weight" && element.name === "value") value = weightValue(weightKg(e)).toFixed(2);
    if (e.type === "workout" && element.name === "distance") value = distanceValue(+e.distance || 0).toFixed(3);
    if (element.tagName === 'SELECT') {
      if (e.type === 'mood' && element.name === 'value')
        value = [...element.options].find((o) => o.value.startsWith(value + ' '))?.value || value;
      if (![...element.options].some((o) => o.value === value))
        element.add(new Option(value, value));
    }
    element.value = value;
  }
  form.querySelector('.primary').textContent = 'Save changes';
}

function barcodeScanned(code) {
  scanFood(code);
}
function scanFood(code = '') {
  currentProduct = null;
  open(
    `<h2>A meal, made simpler.</h2><p>Scan a packaged food or enter its barcode. Internet is used only for the product lookup.</p><button class="primary" data-action="camera">Open camera scanner</button><form id="barcode-form"><label>Product barcode</label><input name="barcode" id="barcode" inputmode="numeric" pattern="[0-9]{8,14}" minlength="8" maxlength="14" value="${esc(code)}" placeholder="8–14 digits" required><button class="secondary">Look up nutrition</button></form><div id="food-result"></div><p class="attribution">Open Food Facts · community-maintained nutrition data.<br>Check results against the package label.</p><button class="link" data-add="food">Enter food manually instead</button>`,
  );
  if (code) lookupFood(code);
}
function lookupFood(code) {
  $('food-result').innerHTML = '<p class="small muted">Looking up this product…</p>';
  Health.lookup(code);
}
function foodError(message) {
  if ($('food-result')) $('food-result').innerHTML = `<div class="notice">${esc(message)}</div>`;
  else toast(message);
}
function foodResult(product) {
  if (!$('food-result')) return;
  currentProduct = product;
  $('food-result').innerHTML =
    `<div class="food-product card"><span class="badge">${product.cached ? 'SAVED PRODUCT' : 'PRODUCT FOUND'}</span><h3>${esc(product.name)}</h3><p class="small muted">${esc(product.brand)} · ${esc(product.basis)}</p><div class="nutrition-line"><strong>${Math.round(product.calories)} kcal</strong><span>P ${product.protein == null ? '—' : (+product.protein).toFixed(1)} · C ${product.carbs == null ? '—' : (+product.carbs).toFixed(1)} · F ${product.fat == null ? '—' : (+product.fat).toFixed(1)} g</span></div><form id="portion-form"><label>Amount eaten (g / ml, as shown on the label)</label><input name="amount" type="number" min="1" max="10000" step="any" value="100" required><label>Date</label><input name="date" type="date" value="${today()}" max="${today()}" required><button class="primary">Log this meal</button></form></div>`;
}

document.addEventListener(
  'submit',
  (event) => {
    const form = event.target;
    if (!form.dataset.edit && !['barcode-form', 'portion-form', 'hydration-form'].includes(form.id))
      return;
    event.preventDefault();
    event.stopImmediatePropagation();
    const data = Object.fromEntries(new FormData(form));
    if (form.dataset.edit) {
      data.type = form.dataset.type;
      normalizeEntryUnits(data);
      if (form.dataset.type === 'mood') data.value = data.value.split(' ')[0];
      const error = Health.edit(form.dataset.edit, JSON.stringify(data));
      if (error) return toast(error);
      state = JSON.parse(Health.state());
      close();
      toast('Changes saved');
    } else if (form.id === 'barcode-form') lookupFood(data.barcode);
    else if (form.id === 'portion-form' && currentProduct) {
      const factor = +data.amount / 100,
        round = (v) => Math.round(v * factor * 10) / 10;
      save({
        type: 'food',
        label: currentProduct.name,
        value: round(currentProduct.calories),
        protein: currentProduct.protein == null ? '' : round(currentProduct.protein),
        carbs: currentProduct.carbs == null ? '' : round(currentProduct.carbs),
        fat: currentProduct.fat == null ? '' : round(currentProduct.fat),
        date: data.date,
        barcode: currentProduct.barcode,
        source: 'barcode',
        note: `${data.amount} g/ml · Open Food Facts`,
      });
    } else if (form.id === 'hydration-form') {
      Health.hydration(data.enabled === 'on', +data.minutes);
      state = JSON.parse(Health.state());
      close();
      toast('Hydration preferences saved');
      if (data.enabled === 'on' && !state.notifications) Health.notifications();
    }
  },
  true,
);

document.addEventListener('change', (event) => {
  if (event.target.id === 'theme-mode') {
    Health.theme(event.target.value);
    state = JSON.parse(Health.state());
    render();
  }
});
document.addEventListener('click', (event) => {
  const b = event.target.closest('button');
  if (!b) return;
  if (b.dataset.edit) return editEntry(b.dataset.edit);
  if (b.dataset.route) {
    const e = state.entries.find((x) => x.id === b.dataset.route);
    if (e)
      open(
        `<h2>${esc(e.label || 'Workout route')}</h2><p>${dateLabel(e.date)} · ${distanceValue(+e.distance || 0).toFixed(2)} ${distanceUnit()} · ${esc(e.value)} minutes</p>${routePreview(e.route)}<button class="secondary" data-action="close">Done</button>`,
      );
    return;
  }
  if (b.dataset.start) {
    open(
      `<h2>Start ${esc(b.dataset.start.toLowerCase())}</h2><p>My Health will use precise location to record your route, including while the screen is off. Routes stay on this phone. Finish your session to stop recording.</p><button class="primary" data-workout="start" data-label="${b.dataset.start}">Start workout</button><button class="secondary" data-action="close">Cancel</button>`,
    );
    return;
  }
  if (b.dataset.workout) {
    Health.workout(b.dataset.workout, b.dataset.label || state.workout?.label || 'Walking');
    close();
    setTimeout(refresh, 700);
    return;
  }
  if (b.dataset.range) {
    reportDays = +b.dataset.range;
    render();
    return;
  }
  if (b.dataset.report) {
    Health.report(b.dataset.report);
    return;
  }
  const action = b.dataset.action;
  if (action === 'scan-food') scanFood();
  else if (action === 'camera') Health.scan();
  else if (action === 'widget') Health.widget();
  else if (action === 'health-permissions') Health.healthConnect(true);
  else if (action === 'health-sync') {
    Health.healthConnect(false);
    setTimeout(refresh, 500);
  } else if (action === 'app-lock') Health.lock(!state.appLock);
  else if (action === 'hydration')
    open(
      `<h2>A gentle water break.</h2><p>Reminders run between 8 am and 9 pm and skip notifications once your daily water goal is met. Android may delay reminders to save battery.</p><form id="hydration-form"><label>Reminders</label><select name="enabled"><option value="on" ${state.hydration ? 'selected' : ''}>On</option><option value="off" ${!state.hydration ? 'selected' : ''}>Off</option></select><label>Frequency</label><select name="minutes">${[30, 60, 90, 120, 180, 240].map((n) => `<option value="${n}" ${n === state.hydrationMinutes ? 'selected' : ''}>Every ${n} minutes</option>`).join('')}</select><button class="primary">Save preferences</button><button type="button" class="secondary" data-action="close">Cancel</button></form>`,
    );
});

matchMedia('(prefers-color-scheme: dark)').addEventListener('change', applyTheme);
setInterval(() => {
  if (!modal && state.workout?.active && !state.workout.paused && $('workout-clock'))
    $('workout-clock').textContent = timeFormat(state.workout.elapsed + Date.now() - snapshotAt);
}, 1000);
