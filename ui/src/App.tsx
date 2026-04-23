import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider, App as AntApp } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { Chat } from '@/pages/Chat';
import './App.css';

const App: React.FC = () => {
  return (
    <BrowserRouter>
      <ConfigProvider locale={zhCN}>
        <AntApp>
          <Routes>
            <Route path="/" element={<Chat />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </AntApp>
      </ConfigProvider>
    </BrowserRouter>
  );
};

export default App;
