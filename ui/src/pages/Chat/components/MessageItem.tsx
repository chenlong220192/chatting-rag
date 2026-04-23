import React from 'react';
import { Avatar, Typography, Progress } from 'antd';
import {
  UserOutlined,
  RobotOutlined,
  ThunderboltboltOutlined,
} from '@ant-design/icons';
import type { ChatMessage } from '@/types/chat';

const { Text, Paragraph } = Typography;

interface MessageItemProps {
  message: ChatMessage;
}

const roleLabel = (role: ChatMessage['role']): string => {
  switch (role) {
    case 'user':
      return '你';
    case 'system':
      return '系统';
    default:
      return 'AI';
  }
};

const avatarIcon = (role: ChatMessage['role']): React.ReactNode => {
  switch (role) {
    case 'user':
      return <UserOutlined />;
    case 'system':
      return <ThunderboltboltOutlined />;
    default:
      return <RobotOutlined />;
  }
};

export const MessageItem: React.FC<MessageItemProps> = ({ message }) => {
  const isUser = message.role === 'user';
  const isSystem = message.role === 'system';

  const contextPct =
    message.contextLimit && message.contextLimit > 0
      ? Math.min(100, ((message.contextUsed ?? 0) / message.contextLimit) * 100)
      : 0;

  return (
    <div className={`message message-${message.role}`}>
      <div className="message-role">
        <Avatar size="small" icon={avatarIcon(message.role)} />
        <Text type="secondary" style={{ marginLeft: 6 }}>
          {roleLabel(message.role)}
        </Text>
      </div>
      <div className="message-content">
        <div className="message-text">
          <Paragraph style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}>
            {message.content || (message.streaming ? '正在生成...' : '')}
          </Paragraph>
        </div>

        {/* References */}
        {!message.streaming && message.references && message.references.length > 0 && (
          <details className="references">
            <summary>引用了 {message.references.length} 条文档</summary>
            {message.references.map((ref, k) => (
              <div key={k} className="ref-item">
                <Text type="secondary" className="ref-score">
                  相关度 {Math.round(ref.score * 100)}%
                </Text>
                <Paragraph type="secondary" style={{ marginBottom: 0, fontSize: 12 }}>
                  {ref.content}
                </Paragraph>
              </div>
            ))}
          </details>
        )}

        {/* Assistant meta: model + context bar */}
        {message.role === 'assistant' && (
          <div className="message-meta">
            {message.model && (
              <Text type="secondary" style={{ fontSize: 12 }}>
                {message.model}
              </Text>
            )}
            {contextPct > 0 && (
              <div className="context-bar-wrap">
                <Text type="secondary" style={{ fontSize: 12 }}>
                  上下文
                </Text>
                <Progress
                  percent={Math.round(contextPct)}
                  size="small"
                  showInfo={false}
                  style={{ width: 120 }}
                />
                <Text type="secondary" style={{ fontSize: 12 }}>
                  {message.contextUsed}/{message.contextLimit}
                </Text>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
