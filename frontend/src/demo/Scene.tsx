import type { ReactNode } from 'react';
import { Icon } from '../components/Icon';
import openApi from '../assets/openapi-specification.svg';
import duke from '../assets/duke-wave.svg';
import github from '../assets/github-lockup.svg';
import pullRequest from '../assets/git-pull-request-16.svg';
import { example } from './story';

function JavaMark() {
  return <span className="a3-java-mark"><img src={duke} alt="" /></span>;
}

function YamlLine({ line, level = 0, className = '', children }: {
  line: number;
  level?: number;
  className?: string;
  children: ReactNode;
}) {
  return (
    <div className={`a3-yaml-line ${className}`} data-level={level}>
      <span>{line}</span><code>{children}</code>
    </div>
  );
}

function Contract() {
  return (
    <article className="a3-contract">
      <header className="a3-spec-head">
        <div><span>Контракт поставщика</span><code>{example.contractFile}</code></div>
        <img src={openApi} alt="OpenAPI Specification" width="76" height="54" />
      </header>
      <div className="a3-operation">
        <code className="a3-verb">{example.method}</code>
        <code>{example.endpoint}</code>
        <code className="a3-http"><span>{example.responseStatus}</span> {example.responseType}</code>
      </div>
      <div className="a3-schema">
        <div className="a3-yaml-indent" />
        <YamlLine line={116}><span className="a3-type">ShipmentResponse</span>:</YamlLine>
        <YamlLine line={117} level={1}>type: <span className="a3-literal">object</span></YamlLine>
        <YamlLine line={118} level={1}>properties:</YamlLine>
        <YamlLine line={119} level={2} className="a3-yaml-root">
          <span className="a3-flat-guide" /><span className="a3-property">tracking</span><span className="a3-underscore">_</span>
          <span className="a3-yaml-number">number:</span><span className="a3-root-colon">:</span>
        </YamlLine>
        <YamlLine line={120} level={3} className="a3-yaml-old-type">type: <span className="a3-literal">{example.fieldType}</span></YamlLine>
        <YamlLine line={120} level={3} className="a3-yaml-new-type">type: <span className="a3-literal">object</span></YamlLine>
        <YamlLine line={121} level={3} className="a3-yaml-nested">properties:</YamlLine>
        <YamlLine line={122} level={4} className="a3-yaml-leaf"><span className="a3-leaf-guide" /></YamlLine>
        <YamlLine line={123} level={5} className="a3-yaml-scalar">type: <span className="a3-literal">{example.fieldType}</span></YamlLine>
        <code className="a3-schema-previous">− {example.removedPath}</code>
      </div>
    </article>
  );
}

function Signature() {
  return <><span className="a3-type">String</span> <span className="a3-method">trackingNumber</span>(<span className="a3-type">Shipment</span> shipment) {'{'}</>;
}

function Expression({ patched }: { patched: boolean }) {
  return <><span className="a3-keyword">return</span> shipment.<span className="a3-method">{patched ? 'tracking' : 'trackingNumber'}</span>(){patched && <>.<span className="a3-method">number</span>()</>};</>;
}

function Editor() {
  return (
    <article className="a3-editor">
      <header className="a3-editor-tabs">
        <div className="a3-current-tab"><JavaMark /><code>{example.javaFile}</code></div>
        <code className="a3-secondary-tab">Shipment.java</code>
      </header>
      <div className="a3-editor-path"><code>{example.sourceFolder}</code><span>/</span><code>{example.javaFile}</code></div>
      <div className="a3-java-lines">
        <div className="a3-java-line"><span>{example.contextLine}</span><code><Signature /></code></div>
        <div className="a3-java-line a3-java-target">
          <span>{example.affectedLine}</span>
          <code><span className="a3-keyword">return</span> shipment.<span className="a3-accessor"><span className="a3-method">tracking</span><span className="a3-accessor-old"><span className="a3-method">Number</span>()</span><span className="a3-accessor-insert">()</span><span className="a3-accessor-new">.<span className="a3-method">number</span>()</span><span className="a3-semicolon">;</span><span className="a3-call-underline" /></span></code>
          <span className="a3-java-marked" />
          <span className="a3-java-edit" />
        </div>
        <div className="a3-java-line"><span>{example.affectedLine + 1}</span><code>{'}'}</code></div>
      </div>
      <footer className="a3-editor-footer">
        <code>Java</code><span className="a3-editor-baseline"><b>×</b> Исходная проверка · номер не сохранён</span>
        <code className="a3-editor-location"><span className="a3-edit-sign">M</span> Стр. {example.affectedLine}</code>
      </footer>
    </article>
  );
}

function Propagation() {
  const route = 'M87 5 H74 C47 5 39 33 29 49 S14 72 14 91 V177 Q14 203 39 203 H62';
  return (
    <svg className="a3-propagation" width="89" height="207" viewBox="0 0 89 207">
      <defs>
        <linearGradient id="a3-trace-ink" gradientUnits="userSpaceOnUse" x1="75" y1="7" x2="15" y2="199">
          <stop /><stop offset=".56" /><stop offset="1" />
        </linearGradient>
      </defs>
      <path className="a3-trace-base" d={route} />
      <path className="a3-trace-ink" pathLength="1" d={route} />
    </svg>
  );
}

function Verification() {
  return (
    <article className="a3-checks">
      <header className="a3-runner"><span className="a3-prompt" aria-hidden="true">›_</span><span>Исполнитель</span><code>{example.case}</code></header>
      <div className="a3-command"><span>$</span><code>{example.command}</code></div>
      <div className="a3-run-log">
        <div className="a3-log-row a3-base-run"><span>Исходный клиент</span><div className="a3-run-output"><span className="a3-fail-symbol">×</span><span>Номер не сохранён</span><code>exit 1</code></div></div>
        <div className="a3-log-row a3-patch-run">
          <span>После адаптации</span>
          <span className="a3-run-pending">Выполняется</span>
          <div className="a3-run-output a3-run-passed"><span className="a3-pass-symbol"><Icon name="check" size={16} /></span><span>Тест пройден</span><code>exit 0</code></div>
        </div>
      </div>
      <div className="a3-assertion">
        <div className="a3-assertion-main">
          <span>Номер в заказе</span>
          <span className="a3-actual-value"><strong className="a3-waiting-number">—</strong><strong className="a3-retained-number">{example.number}</strong></span>
        </div>
        <div className="a3-expected"><span>Ожидалось</span><code>{example.number}</code><span className="a3-assertion-match"><Icon name="check" size={17} /> Совпадает</span></div>
      </div>
    </article>
  );
}

function Publication() {
  return (
    <article className="a3-publication">
      <header className="a3-pr-head">
        <span className="a3-pr-kicker"><img src={pullRequest} width="16" height="16" alt="" /><code>Pull request <b>#{example.pullRequest}</b></code></span>
        <img className="a3-github" src={github} alt="GitHub" width="106" height="24" />
      </header>
      <div className="a3-pr-title">
        <h2>Сохранить трек-номер</h2>
        <div className="a3-pr-context"><span className="a3-pr-open">Открыт</span><span>Ждёт ревью</span></div>
      </div>
      <div className="a3-pr-branch"><Icon name="branch" size={16} /><code>{example.branch}</code><Icon name="arrow" size={15} /><code>{example.target}</code></div>
      <div className="a3-diff">
        <header><span><JavaMark /><code>{example.javaFile}</code></span><span><Icon name="commit" size={15} /><code>{example.commit}</code></span></header>
        <div className="a3-diff-hunk"><code>@@ -41,3 +41,3 @@</code><span><b>+1</b> <b>−1</b></span></div>
        <div className="a3-diff-row"><span>41</span><span>41</span><span /><code><Signature /></code></div>
        <div className="a3-diff-row a3-diff-delete"><span>42</span><span /><span>−</span><code><Expression patched={false} /></code></div>
        <div className="a3-diff-row a3-diff-add"><span /><span>42</span><span>+</span><code><Expression patched /></code></div>
        <div className="a3-diff-row"><span>43</span><span>43</span><span /><code>{'}'}</code></div>
      </div>
      <footer className="a3-pr-evidence">
        <span className="a3-pass-symbol"><Icon name="check" size={18} /></span>
        <span><b>Проверки исполнителя</b><code>{example.command} <span>· exit 0</span></code></span>
        <span><b>{example.number}</b><small>Сохранён в заказе</small></span>
      </footer>
    </article>
  );
}

export function CompactScene({ phase }: { phase: number }) {
  if (phase === 0) {
    return (
      <div className="a3-compact-content">
        <div className="a3-compact-head"><code>{example.contractFile}</code><img src={openApi} alt="" width="65" height="38" /></div>
        <code><span className="a3-method">{example.method}</span> {example.endpoint}</code>
        <div className="a3-compact-diff"><code>− {example.removedPath}</code><code>+ {example.addedPath}</code></div>
      </div>
    );
  }
  if (phase < 3) {
    return (
      <div className="a3-compact-content">
        <div className="a3-compact-head"><JavaMark /><code>{example.javaFile}</code></div>
        <code className="a3-compact-java"><span>{example.affectedLine}</span>shipment.<span className="a3-method">{phase === 1 ? example.oldAccessor : example.newAccessor}</span></code>
        {phase === 1 && <p>Исходная проверка · номер не сохранён</p>}
      </div>
    );
  }
  if (phase < 5) {
    return (
      <div className="a3-compact-content">
        <div className="a3-compact-head"><span className="a3-prompt">›_</span><code>{example.case}</code></div>
        <code>$ {example.command}</code>
        {phase === 3 ? <p>Выполняется</p> : <div className="a3-compact-proof"><span>Сохранён в заказе</span><strong>{example.number}</strong><code>exit 0</code></div>}
      </div>
    );
  }
  return (
    <div className="a3-compact-content">
      <div className="a3-compact-head"><code>Pull request #{example.pullRequest}</code><img className="a3-github" src={github} alt="GitHub" width="80" height="19" /></div>
      <h2>Сохранить трек-номер</h2>
      <p><span className="a3-pr-open">Открыт</span> <code>{example.commit}</code></p>
      <div className="a3-compact-proof"><span>Проверки исполнителя</span><strong>{example.number}</strong><code>exit 0</code></div>
    </div>
  );
}

export function Scene() {
  return (
    <div className="a3-world">
      <Contract />
      <Editor />
      <Propagation />
      <Verification />
      <Publication />
    </div>
  );
}
