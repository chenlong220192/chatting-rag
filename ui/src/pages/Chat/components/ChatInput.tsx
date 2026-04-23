import React from 'react';
import { Input, Button } from 'antd';

const { TextArea } = Input;

interface ChatInputProps {
  value: string;
  loading: boolean;
  onChange: (value: string) => void;
  onSend: () => void;
}

export const ChatInput: React.FC<ChatInputProps> = ({ value, loading, onChange, onSend }) => {
  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>): void => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      onSend();
    }
  };

  return (
    <div className="input-area">
      <TextArea
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder="输入问题，按 Enter 发送..."
        autoSize={{ minRows: 2, maxRows: 6 }}
        disabled={loading}
      />
      <Button
        type="primary"
        onClick={onSend}
        disabled={loading || !value.trim()}
        loading={loading}
      >
        发送
      </Button>
    </div>
  );
};
