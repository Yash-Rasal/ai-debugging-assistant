import { useMemo, useRef, useState } from 'react';
import Editor from '@monaco-editor/react';

const API_URL = '/api/debug';

const starterCode = `int value = 10;
int result = value / 0;
System.out.println(result);`;

const defaultResult = {
  cause: 'Ready to analyze Java code.',
  suggestion: 'Run the debugger to inspect compile-time or runtime feedback.',
  confidence: 0,
  severity: 'LOW',
  category: 'IDLE',
  lineNumber: -1
};

const severityConfig = {
  HIGH: { label: 'High', className: 'high', icon: 'alert' },
  MEDIUM: { label: 'Medium', className: 'medium', icon: 'warning' },
  LOW: { label: 'Low', className: 'low', icon: 'check' }
};

function App() {
  const editorRef = useRef(null);
  const monacoRef = useRef(null);
  const decorationsRef = useRef([]);
  const [code, setCode] = useState(starterCode);
  const [result, setResult] = useState(defaultResult);
  const [isLoading, setIsLoading] = useState(false);
  const [requestState, setRequestState] = useState('Backend: localhost:8080');
  const [activeTab, setActiveTab] = useState('analysis');

  const metrics = useMemo(() => {
    const lines = code.split(/\r?\n/).length;
    const characters = code.length;
    return { lines, characters };
  }, [code]);

  const severity = severityConfig[result.severity] ?? {
    label: result.severity || 'Unknown',
    className: 'neutral',
    icon: 'info'
  };

  const executionStatus = getExecutionStatus(result, isLoading);
  const stackTrace = result.stackTrace || result.trace || '';
  const consoleOutput = result.output || result.consoleOutput || result.logs || '';

  const handleEditorMount = (editor, monaco) => {
    editorRef.current = editor;
    monacoRef.current = monaco;

    monaco.editor.defineTheme('debugger-dark', {
      base: 'vs-dark',
      inherit: true,
      rules: [
        { token: 'keyword', foreground: '7dd3fc' },
        { token: 'number', foreground: 'facc15' },
        { token: 'string', foreground: '86efac' }
      ],
      colors: {
        'editor.background': '#080d1a',
        'editor.lineHighlightBackground': '#101a33',
        'editor.selectionBackground': '#1e3a5f',
        'editorLineNumber.foreground': '#64748b',
        'editorLineNumber.activeForeground': '#e2e8f0',
        'editorCursor.foreground': '#38bdf8',
        'editorGutter.background': '#080d1a',
        'editorIndentGuide.background1': '#1f2a44',
        'editorIndentGuide.activeBackground1': '#3b82f6'
      }
    });

    monaco.editor.setTheme('debugger-dark');
  };

  const highlightLine = (lineNumber) => {
    const editor = editorRef.current;
    const monaco = monacoRef.current;

    if (!editor || !monaco) {
      return;
    }

    const validLine = Number.isInteger(lineNumber) && lineNumber > 0;
    const decorations = validLine
      ? [
          {
            range: new monaco.Range(lineNumber, 1, lineNumber, 1),
            options: {
              isWholeLine: true,
              className: 'error-line-highlight',
              glyphMarginClassName: 'error-line-glyph',
              linesDecorationsClassName: 'error-line-marker',
              marginClassName: 'error-line-margin',
              overviewRuler: {
                color: '#fb7185',
                position: monaco.editor.OverviewRulerLane.Right
              }
            }
          }
        ]
      : [];

    decorationsRef.current = editor.deltaDecorations(decorationsRef.current, decorations);

    if (validLine) {
      editor.revealLineInCenter(lineNumber);
    }
  };

  const runDebug = async () => {
    setIsLoading(true);
    setRequestState('Analyzing with Spring Boot backend...');

    try {
      const response = await fetch(API_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ code })
      });

      if (!response.ok) {
        throw new Error(`Backend returned HTTP ${response.status}`);
      }

      const payload = await response.json();
      setResult(payload);
      highlightLine(payload.lineNumber);
      setRequestState('Analysis complete');
    } catch (error) {
      const fallback = {
        cause: 'Frontend could not reach the debugger backend.',
        suggestion: error.message,
        confidence: 0,
        severity: 'HIGH',
        category: 'CONNECTION_ERROR',
        lineNumber: -1
      };

      setResult(fallback);
      highlightLine(-1);
      setRequestState('Connection failed');
    } finally {
      setIsLoading(false);
    }
  };

  const clearResult = () => {
    setResult(defaultResult);
    highlightLine(-1);
    setRequestState('Backend: localhost:8080');
  };

  return (
    <main className="app-shell">
      <section className="workspace">
        <header className="topbar">
          <div className="title-block">
            <p className="eyebrow">Spring Boot AI Debugger</p>
            <h1>Java Runtime Analysis</h1>
          </div>
          <div className={`status-pill ${isLoading ? 'is-loading' : ''}`}>
            <span className="status-dot" />
            <span>{requestState}</span>
          </div>
        </header>

        <div className="content-grid">
          <section className="editor-panel" aria-label="Java code editor">
            <div className="panel-toolbar">
              <div className="panel-heading">
                <span className="panel-title">Main.java snippet</span>
                <span className="panel-meta">{metrics.lines} lines / {metrics.characters} chars</span>
              </div>
              <div className="toolbar-actions">
                <button className="secondary-button" type="button" onClick={clearResult}>
                  Clear
                </button>
                <button className="primary-button" type="button" onClick={runDebug} disabled={isLoading}>
                  {isLoading ? (
                    <>
                      <span className="button-spinner" />
                      Analyzing
                    </>
                  ) : (
                    'Run Debug'
                  )}
                </button>
              </div>
            </div>

            <div className="editor-frame">
              {isLoading && (
                <div className="analysis-overlay" aria-live="polite">
                  <span className="analysis-spinner" />
                  <span>Analyzing code path</span>
                </div>
              )}
              <Editor
                height="100%"
                language="java"
                theme="debugger-dark"
                value={code}
                onChange={(value) => setCode(value ?? '')}
                onMount={handleEditorMount}
                options={{
                  automaticLayout: true,
                  fontSize: 14,
                  fontFamily: '"JetBrains Mono", "Fira Code", Consolas, monospace',
                  fontLigatures: true,
                  minimap: { enabled: false },
                  glyphMargin: true,
                  lineNumbersMinChars: 3,
                  scrollBeyondLastLine: false,
                  padding: { top: 20, bottom: 20 },
                  roundedSelection: false,
                  tabSize: 4,
                  cursorBlinking: 'smooth',
                  smoothScrolling: true,
                  renderLineHighlight: 'all',
                  bracketPairColorization: { enabled: true },
                  guides: {
                    bracketPairs: true,
                    indentation: true
                  }
                }}
              />
            </div>
          </section>

          <aside className="result-panel" aria-label="Debug analysis result">
            <div className="result-header">
              <div className="panel-heading">
                <span className="panel-title">Structured Response</span>
                <p>Normalized output from <code>/api/debug</code></p>
              </div>
              <span className={`severity-badge ${severity.className}`}>
                <SeverityIcon type={severity.icon} />
                {severity.label}
              </span>
            </div>

            <div className="execution-strip" aria-label="Execution status">
              <StatusIndicator label="Status" value={executionStatus.label} tone={executionStatus.tone} />
              <StatusIndicator label="Endpoint" value="/api/debug" tone="blue" />
              <StatusIndicator label="Line" value={result.lineNumber > 0 ? result.lineNumber : 'N/A'} tone="rose" />
            </div>

            <div className={`analysis-state ${isLoading ? 'active' : ''}`}>
              <span className="analysis-state-bar" />
              <span>{isLoading ? 'Waiting for backend response' : 'Latest analysis result'}</span>
            </div>

            <div className="debug-tabs" role="tablist" aria-label="Debug panels">
              <TabButton id="analysis" activeTab={activeTab} onSelect={setActiveTab}>
                AI Analysis
              </TabButton>
              <TabButton id="stack" activeTab={activeTab} onSelect={setActiveTab}>
                Stack Trace
              </TabButton>
              <TabButton id="console" activeTab={activeTab} onSelect={setActiveTab}>
                Console Output
              </TabButton>
            </div>

            {activeTab === 'analysis' && (
              <div className="tab-panel" role="tabpanel">
                <div className="response-grid">
                  <ResponseCard label="Category" value={result.category} tone="blue" />
                  <ResponseCard label="Confidence" value={`${result.confidence}%`} tone="green" />
                  <ResponseCard label="Severity" value={severity.label} tone="rose" />
                </div>

                <DebugSection title="Root Cause" defaultOpen>
                  <ResponseBlock title="Cause" body={result.cause} />
                </DebugSection>

                <DebugSection title="Recommended Fix" defaultOpen>
                  <ResponseBlock title="Suggestion" body={result.suggestion} />
                </DebugSection>
              </div>
            )}

            {activeTab === 'stack' && (
              <div className="tab-panel" role="tabpanel">
                <DebugSection title="Stack Trace" defaultOpen>
                  <CodeBlock
                    value={stackTrace}
                    emptyText="The current backend response does not include a stack trace field."
                  />
                </DebugSection>
              </div>
            )}

            {activeTab === 'console' && (
              <div className="tab-panel" role="tabpanel">
                <DebugSection title="Console Output" defaultOpen>
                  <CodeBlock
                    value={consoleOutput}
                    emptyText="No console output was returned for this analysis."
                  />
                </DebugSection>

                <DebugSection title="Request Summary">
                  <CodeBlock
                    value={`POST ${API_URL}\nPayload: { code: <${metrics.lines} lines> }\nStatus: ${executionStatus.label}`}
                  />
                </DebugSection>
              </div>
            )}
          </aside>
        </div>
      </section>
    </main>
  );
}

function getExecutionStatus(result, isLoading) {
  if (isLoading) {
    return { label: 'Running', tone: 'blue' };
  }

  if (result.category === 'IDLE') {
    return { label: 'Ready', tone: 'neutral' };
  }

  if (result.category === 'SUCCESS') {
    return { label: 'Success', tone: 'green' };
  }

  if (result.category === 'CONNECTION_ERROR') {
    return { label: 'Connection Error', tone: 'rose' };
  }

  if (result.category?.includes('ERROR')) {
    return { label: 'Error Detected', tone: 'rose' };
  }

  return { label: 'Analyzed', tone: 'blue' };
}

function TabButton({ id, activeTab, onSelect, children }) {
  const isActive = activeTab === id;

  return (
    <button
      className={`tab-button ${isActive ? 'active' : ''}`}
      type="button"
      role="tab"
      aria-selected={isActive}
      onClick={() => onSelect(id)}
    >
      {children}
    </button>
  );
}

function StatusIndicator({ label, value, tone }) {
  return (
    <div className={`status-indicator ${tone}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function ResponseCard({ label, value, tone }) {
  return (
    <div className={`response-card ${tone}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function ResponseBlock({ title, body }) {
  return (
    <div className="response-block">
      <span>{title}</span>
      <p>{body}</p>
    </div>
  );
}

function DebugSection({ title, defaultOpen = false, children }) {
  return (
    <details className="debug-section" open={defaultOpen}>
      <summary>{title}</summary>
      <div className="debug-section-body">{children}</div>
    </details>
  );
}

function CodeBlock({ value, emptyText }) {
  const content = value?.trim();

  return (
    <pre className={`code-block ${content ? '' : 'empty'}`}>
      <code>{content || emptyText}</code>
    </pre>
  );
}

function SeverityIcon({ type }) {
  if (type === 'check') {
    return (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M20 6 9 17l-5-5" />
      </svg>
    );
  }

  if (type === 'warning') {
    return (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="m12 3 10 18H2L12 3Z" />
        <path d="M12 9v5" />
        <path d="M12 17h.01" />
      </svg>
    );
  }

  if (type === 'alert') {
    return (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="12" cy="12" r="10" />
        <path d="M12 7v6" />
        <path d="M12 16h.01" />
      </svg>
    );
  }

  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="12" cy="12" r="10" />
      <path d="M12 11v6" />
      <path d="M12 7h.01" />
    </svg>
  );
}

export default App;
