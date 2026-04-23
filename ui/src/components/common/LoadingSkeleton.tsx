import React from 'react';
import { Skeleton } from 'antd';

export const ChatSkeleton: React.FC = () => {
  return (
    <div style={{ padding: '24px 32px', display: 'flex', flexDirection: 'column', gap: 20 }}>
      {/* Empty state hint skeleton */}
      <div style={{ textAlign: 'center', marginTop: 60 }}>
        <Skeleton.Avatar active size="large" shape="circle" style={{ marginBottom: 12 }} />
        <Skeleton active paragraph={{ rows: 2 }} title={{ width: '40%' }} />
      </div>
      {/* Message skeletons */}
      {[1, 2, 3].map((i) => (
        <div
          key={i}
          style={{
            display: 'flex',
            flexDirection: i % 2 === 0 ? 'row' : 'row-reverse',
            gap: 12,
            alignItems: 'flex-start',
          }}
        >
          <Skeleton.Avatar active size="small" />
          <Skeleton
            active
            paragraph={{ rows: i === 2 ? 3 : 2, width: i === 2 ? '80%' : '60%' }}
            title={false}
          />
        </div>
      ))}
    </div>
  );
};
