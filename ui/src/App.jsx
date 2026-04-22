import { useState, useRef, useEffect } from 'react';
import './App.css';

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api';

export default function App() {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const messagesEndRef = useRef(null);
  const currentAssistantRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const handleSend = async () => {
    if (!input.trim() || loading) return;

    const userMsg = input.trim();
    setInput('');
    setLoading(true);

    // 添加用户消息
    setMessages(prev => [...prev, { role: 'user', content: userMsg }]);

    // 添加一个空的 AI 消息占位，用于流式更新
    const assistantId = Date.now();
    setMessages(prev => [
      ...prev,
      { role: 'assistant', id: assistantId, content: '', references: [], streaming: true, model: '', contextUsed: 0, contextLimit: 32000 }
    ]);
    currentAssistantRef.current = assistantId;

    try {
      const resp = await fetch(`${API_BASE}/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: userMsg })
      });

      if (!resp.ok) throw new Error('请求失败: ' + resp.status);

      const reader = resp.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';
      let references = [];
      let answer = '';
      let currentEvent = null;
      let model = '';
      let contextUsed = 0;
      let contextLimit = 32000;

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        for (const raw of lines) {
          const line = raw.trim();
          if (!line) continue;

          if (line.startsWith('event:')) {
            currentEvent = line.slice(6).trim();
            continue;
          }

          if (line.startsWith('data:')) {
            const data = line.slice(5).trim();
            if (!data) continue;

            if (currentEvent === 'references') {
              try { references = JSON.parse(data); } catch {}
            } else if (currentEvent === 'meta') {
              try {
                const m = JSON.parse(data);
                model = m.model || model;
                contextUsed = m.contextUsed || 0;
                contextLimit = m.contextLimit || 32000;
              } catch {}
            } else if (currentEvent === 'done') {
              // done 事件，无需处理
            } else {
              // 普通 data 行：直接是文本内容（MiniMax 流式 chunk）
              answer += data;
              // 实时更新上下文用量（估算）
              contextUsed = Math.floor((answer.length) / 4);
              setMessages(prev =>
                prev.map(msg =>
                  msg.id === assistantId ? { ...msg, content: answer, model, contextUsed, contextLimit } : msg
                )
              );
            }
            currentEvent = null;
          }
        }
      }

      // 移除 streaming 标记，完成
      setMessages(prev =>
        prev.map(msg =>
          msg.id === assistantId ? { ...msg, content: answer, references, streaming: false, model, contextUsed, contextLimit } : msg
        )
      );
    } catch (e) {
      setMessages(prev =>
        prev.map(msg =>
          msg.id === assistantId
            ? { ...msg, content: '抱歉，请求失败了：' + e.message, streaming: false }
            : msg
        )
      );
    } finally {
      setLoading(false);
      currentAssistantRef.current = null;
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setUploading(true);
    const formData = new FormData();
    formData.append('file', file);
    try {
      const res = await fetch(`${API_BASE}/documents`, { method: 'POST', body: formData });
      if (!res.ok) throw new Error('上传失败');
      const data = await res.json();
      setMessages(prev => [
        ...prev,
        { role: 'system', content: `文档「${data.filename}」上传成功，已索引 ${data.status}` }
      ]);
    } catch (err) {
      setMessages(prev => [
        ...prev,
        { role: 'system', content: '文档上传失败：' + err.message }
      ]);
    } finally {
      setUploading(false);
      e.target.value = '';
    }
  };

  return (
    <div className="container">
      <header className="header">
        <h1>RAG 聊天助手</h1>
        <div className="header-right">
          <label className="upload-btn">
            {uploading ? '上传中...' : '上传文档'}
            <input type="file" accept=".txt,.md" onChange={handleUpload} disabled={uploading} style={{ display: 'none' }} />
          </label>
        </div>
      </header>

      <div className="messages">
        {messages.length === 0 && (
          <div className="empty-hint">
            <span className="empty-icon">💬</span>
            发送消息开始聊天，或先上传文档让 AI 基于文档内容回答
          </div>
        )}

        {messages.map((msg, i) => (
          <div key={i} className={`message message-${msg.role}`}>
            <div className="message-role">
              {msg.role === 'user' ? '你' : msg.role === 'system' ? '系统' : 'AI'}
            </div>
            <div className="message-content">
              <div className="message-text">
                {msg.content.split('\n').map((line, j) => (
                  <p key={j}>{line || <br />}</p>
                ))}
              </div>
              {!msg.streaming && msg.references?.length > 0 && (
                <details className="references">
                  <summary>引用了 {msg.references.length} 条文档</summary>
                  {msg.references.map((ref, k) => (
                    <div key={k} className="ref-item">
                      <span className="ref-score">相关度 {Math.round(ref.score * 100)}%</span>
                      <p>{ref.content}</p>
                    </div>
                  ))}
                </details>
              )}
              {msg.role === 'assistant' && (
                <div className="message-meta">
                  {msg.model && <span className="model-badge">{msg.model}</span>}
                  {msg.contextLimit > 0 && (
                    <div className="context-bar-wrap">
                      <span className="context-label">上下文</span>
                      <div className="context-bar">
                        <div
                          className="context-bar-fill"
                          style={{ width: Math.min(100, (msg.contextUsed / msg.contextLimit) * 100) + '%' }}
                        />
                      </div>
                      <span className="context-label">
                        {msg.contextUsed}/{msg.contextLimit}
                      </span>
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        ))}

        <div ref={messagesEndRef} />
      </div>

      <div className="input-area">
        <textarea
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="输入问题，按 Enter 发送..."
          rows={2}
          disabled={loading}
        />
        <button onClick={handleSend} disabled={loading || !input.trim()}>
          {loading ? '生成中...' : '发送'}
        </button>
      </div>
    </div>
  );
}
