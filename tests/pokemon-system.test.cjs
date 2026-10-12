const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const crypto = require('node:crypto');

const root = path.resolve(__dirname, '..');
const appSource = fs.readFileSync(path.join(root, 'app.js'), 'utf8');
const expectedIds = [1,2,3,4,5,6,7,8,9,25,39,43,58,129,133,134,135,136,152,153,154,155,156,157,158,159,160,196,197,252,253,254,255,256,257,258,259,260,387,388,389,390,391,392,393,394,395,470,471,700];

function runApp(saved = null, randomValue = 0) {
  const store = new Map(saved ? [['pokeroutine-v1', JSON.stringify(saved)]] : []);
  const elements = new Map();
  const makeElement = () => ({
    innerHTML: '', textContent: '', dataset: {}, attrs: {}, onclick: null,
    classList: { add(){}, remove(){}, toggle(){}, contains(){return false} },
    setAttribute(k,v){this.attrs[k]=v}, removeAttribute(k){delete this.attrs[k]}
  });
  const document = {
    querySelector(selector) { if (!elements.has(selector)) elements.set(selector, makeElement()); return elements.get(selector); },
    querySelectorAll() { return []; },
    addEventListener() {},
    createElement() { return makeElement(); }
  };
  const math = Object.create(Math); math.random = () => randomValue;
  const context = {
    document, localStorage: {getItem:k=>store.get(k)||null,setItem:(k,v)=>store.set(k,v)},
    navigator: {}, location: {protocol:'http:'}, crypto: crypto.webcrypto,
    setTimeout:()=>1, clearTimeout(){}, console, Math:math,
  };
  context.window=context;
  const testHook = '\nwindow.__pokeroutineTest={state,species,starters,LINES,EVOLUTION_LEVELS,familyBase,level,currentMon,evolveProgress,selectStarter,randomStarter,evolveIfReady,toggleTask,applyBackup,renderOnboarding};\n';
  const instrumented = appSource.replace(/\}\)\(\);\s*$/, testHook+'})();');
  vm.runInNewContext(instrumented, context, {filename:'app.js'});
  return {api:context.__pokeroutineTest,store,context};
}

test('catálogo tem as 50 espécies e todas as 12 escolhas iniciais', () => {
  const {api}=runApp();
  assert.equal(api.species.length, 50);
  assert.equal(api.starters.length, 12);
  assert.deepEqual(Array.from(api.starters, p=>p.id), [1,4,7,152,155,158,252,255,258,387,390,393]);
  for (const chain of [[1,2,3],[4,5,6],[7,8,9],[152,153,154],[155,156,157],[158,159,160],[252,253,254],[255,256,257],[258,259,260],[387,388,389],[390,391,392],[393,394,395]]) {
    assert.deepEqual(Array.from(api.LINES[chain[0]]), chain);
  }
  assert.deepEqual(Array.from(api.LINES[133][1]), [134,135,136,196,197,470,471,700]);
  assert.deepEqual({...api.EVOLUTION_LEVELS},{first:5,second:10,eevee:5});
});

test('todos os sprites existem no app web e na cópia Android e são PNGs válidos', () => {
  for (const id of expectedIds) for (const folder of ['pokemon','android/app/src/main/assets/pokemon']) {
    const file=path.join(root,folder,`${id}.png`);
    const bytes=fs.readFileSync(file);
    assert.equal(bytes.subarray(0,8).toString('hex'),'89504e470d0a1a0a',file);
    assert.equal(bytes.readUInt32BE(16),96,file);
    assert.equal(bytes.readUInt32BE(20),96,file);
  }
  for(const file of ['app.js','styles.css','index.html','sw.js','manifest.webmanifest']){
    assert.deepEqual(fs.readFileSync(path.join(root,file)),fs.readFileSync(path.join(root,'android/app/src/main/assets',file)),`${file} divergente no Android`);
  }
});

test('service worker prepara os mesmos 50 sprites para uso offline', () => {
  const sw=fs.readFileSync(path.join(root,'sw.js'),'utf8');
  const match=sw.match(/const POKEMON_IDS=(\[[^\]]+\])/);
  assert.ok(match,'lista de sprites não encontrada no service worker');
  assert.deepEqual(JSON.parse(match[1]),expectedIds);
  assert.match(sw,/pokeroutine-shell-v4/);
  assert.match(sw,/\.\/assets\/pokeball-commons\.svg/);
  assert.ok(fs.existsSync(path.join(root,'assets/pokeball-commons.svg')),'asset Poké Bola offline não encontrado');
  const workflow=fs.readFileSync(path.join(root,'.github/workflows/android-debug.yml'),'utf8');
  const workflowIds=workflow.match(/pokemon_ids=\(([^)]+)\)/);
  assert.ok(workflowIds,'workflow sem verificação de sprites no APK');
  assert.deepEqual(workflowIds[1].trim().split(/\s+/).map(Number),expectedIds);
});

test('migração mantém tarefas, XP, Pokébolas e companheiro legado', () => {
  const legacy={tasks:[{id:'t1',title:'Estudar',done:false}],xp:275,balls:8,starter:'Squirtle',found:['Squirtle','Eevee'],settings:{sound:false},achievements:['cem']};
  const {api,store}=runApp(legacy);
  assert.equal(api.state.xp,275); assert.equal(api.state.balls,8);
  assert.equal(api.state.tasks[0].title,'Estudar'); assert.equal(api.currentMon().name,'Squirtle');
  assert.deepEqual(Array.from(api.state.found),['Squirtle','Eevee']);
  const persisted=JSON.parse(store.get('pokeroutine-v1'));
  assert.equal(persisted.companionId,7); assert.equal(persisted.lineages['7'],7);
});

test('seleção manual dos iniciais preserva a espécie escolhida', () => {
  const {api,store}=runApp();
  api.selectStarter(393);
  assert.equal(api.currentMon().name,'Piplup');
  assert.ok(api.state.found.includes('Piplup'));
  assert.equal(JSON.parse(store.get('pokeroutine-v1')).companionId,393);
});

test('sorteio inicial escolhe uma das 12 espécies e mostra escolha manual por geração', () => {
  const {api,context}=runApp(null,.999);
  api.renderOnboarding();
  const html=context.document.querySelector('#view').innerHTML;
  assert.equal((html.match(/data-starter-id=/g)||[]).length,12);
  for(let generation=1;generation<=4;generation++)assert.ok(html.includes(`GERAÇÃO ${generation}`));
  api.randomStarter();
  assert.equal(api.currentMon().name,'Piplup');
});

test('as 12 linhas evoluem no nível 5 e no nível 10', () => {
  for(const base of [1,4,7,152,155,158,252,255,258,387,390,393]){
    const p=runApp({xp:0,balls:3,starter:null,found:[],tasks:[]});
    p.api.selectStarter(base);
    p.api.state.xp=400;
    assert.equal(p.api.evolveIfReady().length,1,`base #${base} não evoluiu no nível 5`);
    assert.equal(p.api.state.lineages[String(base)],p.api.LINES[base][1]);
    p.api.state.xp=900;
    assert.equal(p.api.evolveIfReady().length,1,`base #${base} não evoluiu no nível 10`);
    assert.equal(p.api.state.lineages[String(base)],p.api.LINES[base][2]);
  }
});

test('evoluções comuns respeitam nível 5 e nível 10, sem pular estágios e persistem', () => {
  const saved={xp:399,balls:3,starter:'Squirtle',found:['Squirtle'],tasks:[]};
  const first=runApp(saved);
  assert.equal(first.api.evolveIfReady().length,0);
  first.api.state.xp=400;
  const firstStep=first.api.evolveIfReady();
  assert.deepEqual(Array.from(firstStep,e=>[e.fromId,e.toId]),[[7,8]]);
  assert.equal(first.api.currentMon().name,'Wartortle');
  assert.ok(first.api.state.found.includes('Wartortle'));
  const reopened=runApp(JSON.parse(first.store.get('pokeroutine-v1')));
  assert.equal(reopened.api.currentMon().name,'Wartortle');
  assert.equal(reopened.api.evolveIfReady().length,0);
  reopened.api.state.xp=899;
  assert.equal(reopened.api.evolveIfReady().length,0);
  reopened.api.state.xp=900;
  assert.deepEqual(Array.from(reopened.api.evolveIfReady(),e=>[e.fromId,e.toId]),[[8,9]]);
  assert.equal(reopened.api.currentMon().name,'Blastoise');
  assert.equal(reopened.api.state.evolutionLog.length,2);
});

test('barra de evolução mede XP até o requisito exato', () => {
  const {api}=runApp({xp:0,balls:3,starter:'Bulbasaur',found:['Bulbasaur'],tasks:[]});
  assert.equal(api.evolveProgress().progress,0);
  api.state.xp=200; assert.equal(api.evolveProgress().progress,50);
  api.state.xp=399; assert.equal(api.evolveProgress().progress,100);
  api.state.xp=400; assert.equal(api.evolveIfReady().length,1);
  assert.equal(api.evolveProgress().target,10);
  assert.equal(api.evolveProgress().progress,44);
});

test('Eevee sorteia cada uma das oito formas e não sorteia novamente após reabrir', () => {
  for(let index=0;index<8;index++){
    const saved={xp:399,balls:3,starter:'Eevee',found:['Eevee'],tasks:[],companionId:133,companionBaseId:133,lineages:{133:133},evolutionLog:[]};
    const value=(index+.25)/8;
    const first=runApp(saved,value);
    first.api.state.xp=400;
    const result=first.api.evolveIfReady();
    assert.equal(result.length,1);
    assert.equal(result[0].toId,[134,135,136,196,197,470,471,700][index]);
    const persisted=JSON.parse(first.store.get('pokeroutine-v1'));
    const reopened=runApp(persisted, .99);
    assert.equal(reopened.api.currentMon().id,result[0].toId);
    assert.equal(reopened.api.evolveIfReady().length,0);
    assert.equal(reopened.api.state.evolutionLog.length,1);
    assert.ok(reopened.api.state.found.includes(result[0].to));
  }
});

test('reconcluir uma tarefa não duplica XP nem recompensa surpresa', () => {
  const saved={xp:0,balls:3,starter:'Bulbasaur',found:['Bulbasaur'],tasks:[{id:'task-1',title:'Ler',done:false,xpClaimed:false,surpriseClaimed:false,priority:'Normal',repeat:'Nunca'}]};
  const {api}=runApp(saved,0);
  api.toggleTask('task-1');
  assert.equal(api.state.xp,15); assert.equal(api.state.balls,4);
  api.toggleTask('task-1'); api.toggleTask('task-1');
  assert.equal(api.state.xp,15); assert.equal(api.state.balls,4);
  assert.equal(api.state.tasks[0].surpriseReward,'ball');
});
