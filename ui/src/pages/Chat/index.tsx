import React, { useState } from 'react';
import { Upload, Button, Typography, Empty } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import { ChatInput } from './components/ChatInput';
import { MessageItem } from './components/MessageItem';
import { ChatSkeleton } from '@/components/common/LoadingSkeleton';
import { useChatStore } from '@/stores/chatStore';
import { streamChat } from '@/services/chatService';
import { uploadDocument } from '@/services/documentService';
import { useScrollToBottom } from '@/hooks/useScrollToBottom';

const { Title } = Typography;

export const Chat: React.FC = () => {
  const [input, setInput] = useState('');
  const { messages, loading, uploading, addMessage, updateMessage, setLoading, setUploading } =
    useChatStore();
  const messagesEndRef = useScrollToBottom();

  const handleSend = (): void => {
    if (!input.trim() || loading) return;

    const userMsg = input.trim();
    setInput('');
    setLoading(true);

    // Add user message
    addMessage({ role: 'user', content: userMsg });

    // Add placeholder for AI response
    const assistantId = Date.now();
    addMessage({
      id: assistantId,
      role: 'assistant',
      content: '',
      references: [],
      streaming: true,
      model: '',
      contextUsed: 0,
      contextLimit: 32000,
    });

    streamChat(userMsg, {
      onAnswer: (text) => {
        updateMessage(assistantId, { content: text, contextUsed: Math.floor(text.length / 4) });
      },
      onReferences: (references) => {
        updateMessage(assistantId, { references });
      },
      onMeta: (meta) => {
        updateMessage(assistantId, {
          model: meta.model,
          contextUsed: meta.contextUsed,
          contextLimit: meta.contextLimit,
        });
      },
      onDone: () => {
        updateMessage(assistantId, { streaming: false });
      },
      onError: (error) => {
        updateMessage(assistantId, {
          content: '抱歉，请求失败了：' + error.message,
          streaming: false,
        });
      },
    }).finally(() => {
      setLoading(false);
    });
  };

  const handleUpload = async (file: File): Promise<void> => {
    setUploading(true);
    try {
      const data = await uploadDocument(file);
      addMessage({
        role: 'system',
        content: `文档「${data.filename}」上传成功，已索引 ${data.status}`,
      });
    } catch (err) {
      addMessage({
        role: 'system',
        content: '文档上传失败：' + (err instanceof Error ? err.message : String(err)),
      });
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="container">
      <header className="header">
        <Title level={3} style={{ margin: 0 }}>
          RAG 聊天助手
        </Title>
        <div className="header-right">
          <Upload
            accept=".txt,.md"
            showUploadList={false}
            beforeUpload={(file) => {
              void handleUpload(file);
              return false; // prevent auto upload
            }}
            disabled={uploading}
          >
            <Button icon={<UploadOutlined />} loading={uploading}>
              {uploading ? '上传中...' : '上传文档'}
            </Button>
          </Upload>
        </div>
      </header>

      <div className="messages">
        {messages.length === 0 && !loading && (
          <Empty
            image="https://gw.alipayobjects.com/zos/rmsportal/GvqYWEKkNqXjkEApHJoL.png"
            description={
              <span>
                发送消息开始聊天，或先上传文档让 AI 基于文档内容回答
              </span>
            }
          />
        )}

        {loading && messages.length === 0 && <ChatSkeleton />}

        {messages.map((msg, i) => (
          <MessageItem key={i} message={msg} />
        ))}

        <div ref={messagesEndRef} />
      </div>

      <ChatInput value={input} loading={loading} onChange={setInput} onSend={handleSend} />
    </div>
  );
};

export default Chat;
