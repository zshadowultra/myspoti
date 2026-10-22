// Executes the shipped bridge with deterministic browser/service doubles; no account needed.
// Use --source=/tmp/before.js for the same mechanism counts against a previous revision.
const { readFileSync } = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const sourcePath = process.argv.find(a => a.startsWith('--source='))?.slice(9);
const source = readFileSync(sourcePath || `${__dirname}/../src/main/resources/bridge/player-bridge.js`, 'utf8');

function bootScenario(descendant = false) {
  let queries = 0, mutations, disconnected = false;
  const messages = [], timers = [];
  const player = { getState: () => null, getEvents: () => ({ addListener() {} }) };
  const registry = { _map: new Map(), resolve: key => key.description === 'PlayerAPI' ? player : {} };
  for (const description of ['PlayerAPI', 'PlaybackAPI', 'LibraryAPI', 'TokenProvider']) {
    registry._map.set({ description }, {});
  }
  const platform = { getRegistry: () => registry, getGraphQLLoader: () => async () => ({ data: {} }) };
  const fiber = { memoizedProps: {} };
  const root = { '__reactContainer$test': fiber };
  const host = descendant ? {} : root;
  host.querySelectorAll = () => { queries++; return descendant ? [root] : []; };
  const window = { __mspHostPost: raw => messages.push(JSON.parse(raw)), addEventListener() {} };
  const sandbox = {
    window, document: { getElementById: () => host, body: host }, location: { pathname: '/preferences' },
    MutationObserver: class { constructor(cb) { mutations = cb; } observe() {} disconnect() { disconnected = true; } },
    PerformanceObserver: class { observe() {} },
    setTimeout: cb => { timers.push(cb); }, clearTimeout() {}, console, Date,
  };
  vm.runInNewContext(source, sandbox);
  for (let i = 0; i < 20; i++) timers.shift()();
  if (mutations) { for (let i = 0; i < 100; i++) mutations(); }
  fiber.memoizedProps.value = platform;
  timers.shift()();
  assert(messages.some(m => m.type === 'ready'), 'late service registry must still boot');
  return { queries, disconnected, window, messages };
}

(async () => {
  const direct = bootScenario();
  const nested = bootScenario(true);
  console.log(`boot root: descendant queries=${direct.queries}; fallback + 100 mutations=${nested.queries}`);
  if (!sourcePath) {
    assert.equal(direct.queries, 1); // one pre-mount fallback, none on the subsequent 21 retries
    assert.equal(nested.queries, 2); // first snapshot, then ONE dirty rescan for the burst
    assert(direct.disconnected && nested.disconnected, 'boot observer must be released');
  }
  const result = await direct.window.Myspoti.call(42, 'gql.query', { name: 'test', hash: 'test' });
  assert.deepEqual(result, { data: {} });
  assert(direct.messages.some(m => m.type === 'result' && m.id === 42 && m.ok));
  console.log('PASS actual bridge boot, scoped/coalesced lookup and async request-ID result');
})().catch(e => { console.error(e); process.exitCode = 1; });
