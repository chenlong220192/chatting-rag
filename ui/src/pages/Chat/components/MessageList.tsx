import React, { useEffect, useRef } from 'react';
import { Empty } from 'antd';
import { MessageItem } from './MessageItem';
import { useChatStore } from '@/stores/chatStore';

export const MessageList: React.FC = () => {
  const { messages } = useChatStore();
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  if (messages.length === 0) {
    return (
      <div
        style={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          color: '#aaa',
        }}
      >
        <Empty
          description={
            <span>
              发送消息开始聊天，或先上传文档让 AI 基于文档内容回答
            </span>
          }
          image={Empty.PRESENTED_IMAGE_SIMPLE}
        />
      </div>
    );
  }

  return (
    <div
      style={{
        flex: 1,
        overflowY: 'auto',
        padding: '24px 32px',
        display: 'flex',
        flexDirection: 'column',
        gap: 20,
      }}
    >
      <style>{`
        @keyframes fadeIn {
          from { opacity: 0; transform: translateY(6px); }
          to { opacity: 1; transform: translateY(0); }
        }
        @keyframes blink {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.3; }
        }
      `}</style>
      {messages.map((msg, i) => (
        <MessageItem key={i} message={msg} />
      ))}
      <div ref={bottomRef} />
    </div>
  );
};
