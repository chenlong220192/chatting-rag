import React from 'react';
import { Spin } from 'antd';

interface LoadingProps {
  tip?: string;
  fullscreen?: boolean;
}

export const Loading: React.FC<LoadingProps> = ({ tip = '加载中...', fullscreen = false }) => {
  if (fullscreen) {
    return (
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          height: '100vh',
          width: '100vw',
        }}
      >
        <Spin size="large" tip={tip} />
      </div>
    );
  }
  return <Spin size="large" tip={tip} />;
};
