const fs = require('node:fs');
(async () => {
  const targets = await (await fetch('http://127.0.0.1:9222/json')).json();
  const ws = new WebSocket(
    targets.find((t) => t.url.includes('app.health.local')).webSocketDebuggerUrl,
  );
  await new Promise((r, j) => {
    ws.onopen = r;
    ws.onerror = j;
  });
  ws.onmessage = (e) => {
    const m = JSON.parse(e.data);
    if (m.id === 1) {
      if (m.result?.exceptionDetails) {
        console.error(m.result.exceptionDetails);
        process.exitCode = 1;
      } else console.log(JSON.stringify(m.result?.result?.value));
      ws.close();
    }
  };
  ws.send(
    JSON.stringify({
      id: 1,
      method: 'Runtime.evaluate',
      params: {
        expression: fs.readFileSync(process.argv[2], 'utf8'),
        returnByValue: true,
        awaitPromise: true,
      },
    }),
  );
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
