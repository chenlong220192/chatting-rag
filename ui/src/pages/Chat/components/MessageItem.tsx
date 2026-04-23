import React from 'react';
import { Avatar, Typography, Progress, Collapse, Tag, Tooltip } from 'antd';
import {
  UserOutlined,
  RobotOutlined,
  BulbOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons';
import type { ChatMessage } from '@/types/chat';

const { Text } = Typography;

const roleIcon = {
  user: <UserOutlined />,
  assistant: <RobotOutlined />,
  system: <BulbOutlined />,
};

const roleLabel = {
  user: '你',
  assistant: 'AI',
  system: '系统',
};

interface MessageItemProps {
  message: ChatMessage;
}

export const MessageItem: React.FC<MessageItemProps> = ({ message }) => {
  const isUser = message.role === 'user';
  const isSystem = message.role === 'system';

  const bubbleStyle: React.CSSProperties = isUser
    ? {
        color: '#fff',
        background: '#1677ff',
        padding: '8px 12px',
        borderRadius: '12px 12px 4px 12px',
        boxShadow: '0 2px 8px rgba(22,119,255,0.3)',
        whiteSpace: 'pre-wrap',
        wordBreak: 'break-word',
        maxWidth: '100%',
      }
    : isSystem
    ? {
        color: '#333',
        background: '#fff8e1',
        padding: '8px 12px',
        borderRadius: 8,
        whiteSpace: 'pre-wrap',
        wordBreak: 'break-word',
        maxWidth: '100%',
      }
    : {
        color: '#333',
        background: '#f5f5f5',
        padding: '8px 12px',
        borderRadius: '4px 12px 12px 4px',
        boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
        whiteSpace: 'pre-wrap',
        wordBreak: 'break-word',
        maxWidth: '100%',
      };

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: isUser ? 'row-reverse' : 'row',
        alignItems: 'flex-start',
        gap: 12,
        animation: 'fadeIn 0.2s ease',
      }}
    >
      <Avatar
        icon={roleIcon[message.role]}
        style={{
          background: isUser ? '#1677ff' : '#999',
          flexShrink: 0,
        }}
      />
      <div style={{ maxWidth: '75%' }}>
        {/* Author label */}
        <div style={{ marginBottom: 4, fontWeight: 500, color: isUser ? '#fff' : '#333', fontSize: 13 }}>
          {roleLabel[message.role]}
        </div>

        {/* Content bubble */}
        <div style={bubbleStyle}>
          {message.content || (message.streaming ? <Text type="secondary" italic>正在生成...</Text> : null)}
        </div>

        {/* Streaming indicator */}
        {message.streaming && (
          <div style={{ marginTop: 4 }}>
            <Text type="secondary" style={{ fontSize: 12 }}>
              <span style={{ display: 'inline-block', animation: 'blink 1s infinite' }}>●</span> AI 正在输入
            </Text>
          </div>
        )}

        {/* Meta: model + context bar */}
        {message.role === 'assistant' && !message.streaming && (
          <div style={{ marginTop: 8, display: 'flex', alignItems: 'center', gap: 12 }}>
            {message.model && (
              <Tag icon={<CheckCircleOutlined />} color="blue" style={{ fontSize: 11 }}>
                {message.model}
              </Tag>
            )}
            {message.contextLimit && message.contextLimit > 0 && (
              <Tooltip title={`已使用 ${message.contextUsed}/${message.contextLimit} tokens`}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, flex: 1, maxWidth: 200 }}>
                  <span style={{ fontSize: 11, color: '#999' }}>上下文</span>
                  <Progress
                    percent={Math.min(100, Math.round(((message.contextUsed || 0) / message.contextLimit) * 100))}
                    size="small"
                    strokeColor="#1677ff"
                    style={{ flex: 1 }}
                  />
                </div>
              </Tooltip>
            )}
          </div>
        )}

        {/* References */}
        {message.references && message.references.length > 0 && (
          <Collapse
            ghost
            size="small"
            style={{ marginTop: 8 }}
            items={[
              {
                key: 'refs',
                label: (
                  <span style={{ fontSize: 12, color: '#666' }}>
                    引用了 {message.references.length} 条文档
                  </span>
                ),
                children: (
                  <div>
                    {message.references.map((ref, i) => (
                      <div
                        key={i}
                        style={{
                          marginBottom: 8,
                          padding: '6px 10px',
                          background: 'rgba(0,0,0,0.04)',
                          borderRadius: 6,
                          fontSize: 12,
                        }}
                      >
                        <div style={{ marginBottom: 2 }}>
                          <Tag color="blue" style={{ fontSize: 11 }}>
                            相关度 {Math.round(ref.score * 100)}%
                          </Tag>
                        </div>
                        <Text type="secondary" style={{ fontSize: 12 }}>{ref.content}</Text>
                      </div>
                    ))}
                  </div>
                ),
              },
            ]}
          />
        )}
      </div>
    </div>
  );
};
