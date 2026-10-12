const fs = require('node:fs'),
  assert = require('node:assert/strict');
(async () => {
  const target = (await (await fetch('http://127.0.0.1:9222/json')).json()).find((t) =>
    t.url.includes('app.health.local'),
  );
  const ws = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise((r, j) => {
    ws.onopen = r;
    ws.onerror = j;
  });
  let id = 0;
  const pending = new Map();
  ws.onmessage = (e) => {
    let m = JSON.parse(e.data);
    if (pending.has(m.id)) {
      let p = pending.get(m.id);
      pending.delete(m.id);
      m.error ? p.reject(m.error) : p.resolve(m.result);
    }
  };
  const evaluate = (expression) =>
    new Promise((resolve, reject) => {
      let key = ++id;
      pending.set(key, {
        resolve: (r) => {
          if (r.exceptionDetails) reject(r.exceptionDetails);
          else resolve(r.result.value);
        },
        reject,
      });
      ws.send(
        JSON.stringify({
          id: key,
          method: 'Runtime.evaluate',
          params: { expression, returnByValue: true, awaitPromise: true },
        }),
      );
    });
  const wait = (ms) => new Promise((r) => setTimeout(r, ms));
  const snapshot = async () => JSON.parse(await evaluate('Health.state()'));
  const before = await snapshot();
  if (before.workout.active) {
    console.log('SKIP: user workout already active');
    ws.close();
    return;
  }
  let started = false;
  try {
    await evaluate('Health.workout("start","SMOKE TEST WALK")');
    started = true;
    await wait(2200);
    assert((await snapshot()).workout.active, 'GPS service starts');
    await evaluate('Health.workout("pause","SMOKE TEST WALK")');
    await wait(300);
    let paused = (await snapshot()).workout;
    assert(paused.paused, 'pause state persists');
    let duration = paused.elapsed;
    await wait(1300);
    assert.equal((await snapshot()).workout.elapsed, duration, 'pause freezes duration');
    await evaluate('Health.workout("resume","SMOKE TEST WALK")');
    await wait(1300);
    assert(!(await snapshot()).workout.paused, 'resume state persists');
    assert((await snapshot()).workout.elapsed > duration, 'resume continues timing');
    await evaluate('Health.workout("finish","SMOKE TEST WALK")');
    await wait(500);
    started = false;
    const after = await snapshot();
    assert(!after.workout.active, 'finish stops session');
    let entry = after.entries.find((e) => e.id === paused.id);
    assert(entry, 'workout saved once');
    assert.equal(entry.source, 'gps');
    assert(Array.isArray(entry.route));
    await evaluate(`Health.delete(${JSON.stringify(paused.id)})`);
    assert.equal(
      (await snapshot()).entries.length,
      before.entries.length,
      'temporary workout removed',
    );
    console.log(
      'PASS: GPS foreground session start, pause, stable paused duration, resume, finish/save and cleanup. No simulated locations used.',
    );
  } finally {
    if (started) {
      let w = (await snapshot()).workout;
      await evaluate('Health.workout("finish","SMOKE TEST WALK")');
      await wait(500);
      if (w.id) await evaluate(`Health.delete(${JSON.stringify(w.id)})`);
    }
    await evaluate('refresh();go("home")');
    ws.close();
  }
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
