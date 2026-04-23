import React, { useState, useRef, useEffect } from 'react';
import { Input, Button, Space } from 'antd';
import { SendOutlined } from '@ant-design/icons';
import { useChatStore } from '@/stores/chatStore';

const { TextArea } = Input;

export const ChatInput: React.FC = () => {
  const [value, setValue] = useState('');
  const { sendMessage, isLoading } = useChatStore();
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const handleSend = async () => {
    if (!value.trim() || isLoading) return;
    const content = value.trim();
    setValue('');
    await sendMessage(content);
    textareaRef.current?.focus();
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  // Auto-resize textarea
  useEffect(() => {
    const textarea = textareaRef.current;
    if (textarea) {
      textarea.style.height = 'auto';
      textarea.style.height = `${Math.min(textarea.scrollHeight, 120)}px`;
    }
  }, [value]);

  return (
    <div
      style={{
        padding: '16px 24px 20px',
        borderTop: '1px solid #f0f0f0',
        background: '#fff',
        flexShrink: 0,
      }}
    >
      <Space.Compact style={{ width: '100%' }} size="middle">
        <TextArea
          value={value}
          onChange={(e) => setValue(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="输入问题，按 Enter 发送，Shift+Enter 换行..."
          autoSize={{ minRows: 1, maxRows: 5 }}
          disabled={isLoading}
          style={{ flex: 1, borderRadius: 8 }}
        />
        <Button
          type="primary"
          icon={<SendOutlined />}
          onClick={handleSend}
          loading={isLoading}
          disabled={!value.trim()}
          style={{ height: 'auto', minHeight: 40, borderRadius: 8 }}
        >
          {isLoading ? '生成中' : '发送'}
        </Button>
      </Space.Compact>
    </div>
  );
};
