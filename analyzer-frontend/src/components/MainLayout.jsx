import React from 'react';
import '../App.css'; 

function MainLayout({ sidebar, content }) {
  return (
    <div className="App">
      
      {/* 1. 왼쪽 사이드바 슬롯 */}
      {sidebar}

      {/* 2. 오른쪽 메인 콘텐츠 슬롯 */}
      <div className="main-content">
        <h1>🎨 GitHub AI 요약기 🧑‍💻</h1>
        {content}
      </div>

    </div>
  );
}

export default MainLayout;