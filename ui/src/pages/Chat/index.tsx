import React from 'react';
import { MessageList } from './components/MessageList';
import { ChatInput } from './components/ChatInput';

export const ChatPage: React.FC = () => {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}>
      <MessageList />
      <ChatInput />
    </div>
  );
};
