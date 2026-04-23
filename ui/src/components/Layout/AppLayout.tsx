import React from 'react';
import { Layout, Typography, Button, Space, Upload, message } from 'antd';
import { UploadOutlined, ClearOutlined } from '@ant-design/icons';
import type { UploadProps } from 'antd';
import { useChatStore } from '@/stores/chatStore';

const { Header, Content, Footer } = Layout;
const { Title } = Typography;

interface AppLayoutProps {
  children: React.ReactNode;
}

export const AppLayout: React.FC<AppLayoutProps> = ({ children }) => {
  const { isUploading, uploadDocument, clearMessages, messages } = useChatStore();

  const uploadProps: UploadProps = {
    accept: '.txt,.md,.pdf,.doc,.docx,.csv,.html,.json,.xml',
    showUploadList: false,
    beforeUpload: (file) => {
      if (file.size > 10 * 1024 * 1024) {
        message.error('文件大小不能超过 10MB');
        return false;
      }
      uploadDocument(file);
      return false; // prevent auto upload
    },
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0 24px',
          background: '#001529',
        }}
      >
        <Title level={4} style={{ color: '#fff', margin: 0, lineHeight: '64px' }}>
          RAG 聊天助手
        </Title>
        <Space>
          <Upload {...uploadProps}>
            <Button
              type="primary"
              icon={<UploadOutlined />}
              loading={isUploading}
              style={{ background: '#1677ff' }}
            >
              {isUploading ? '上传中...' : '上传文档'}
            </Button>
          </Upload>
          <Button
            icon={<ClearOutlined />}
            onClick={clearMessages}
            disabled={messages.length === 0}
          >
            清空对话
          </Button>
        </Space>
      </Header>
      <Content style={{ display: 'flex', flexDirection: 'column' }}>
        {children}
      </Content>
      <Footer style={{ textAlign: 'center', padding: '12px', color: '#999', fontSize: 12 }}>
        chatting-rag · Powered by ChromaDB + MiniMax
      </Footer>
    </Layout>
  );
};
