import React, { useState, useEffect } from 'react'; 
import axios from 'axios'; 
import '../styles/Sidebar.css'; 

function Sidebar({ onSearch, onFilter, activeFilter }) { 
  const [searchTerm, setSearchTerm] = useState('');
  const [topics, setTopics] = useState([]);
  const [analyzeUrl, setAnalyzeUrl] = useState(''); 
  const [isLoading, setIsLoading] = useState(false); 
  const [message, setMessage] = useState(null); 

  useEffect(() => {
    const fetchTopics = async () => {
      try {
        const response = await axios.get('http://localhost:8080/api/projects/topics');
        const fetchedTopics = response.data || [];
        setTopics(fetchedTopics.filter(t => t !== 'On-Demand')); 
      } catch (err) {
        console.error("토픽 목록 로딩 에러:", err);
        setTopics([]); 
      }
    };
    fetchTopics();
  }, []); 

  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value);
  };
  const handleSearchSubmit = (e) => {
    e.preventDefault(); 
    onSearch(searchTerm); 
  };

  const handleFilterClick = (filterValue) => {
    onFilter(filterValue); 
  };
  
  const handleAnalyzeSubmit = async (e) => {
    e.preventDefault();
    if (!analyzeUrl.startsWith('https://github.com/')) {
      setMessage({ type: 'error', text: 'https://github.com/... 형식의 URL만 100% 가능합니다.' });
      return;
    }
    setIsLoading(true); 
    setMessage(null); 
    try {
      const response = await axios.post('http://localhost:8080/api/projects/analyze', { url: analyzeUrl });
      setMessage({ type: 'success', text: response.data.message }); 
      setAnalyzeUrl(''); 
    } catch (err) {
      console.error("즉시 분석 요청 에러:", err);
      if (err.response && err.response.data && err.response.data.message) {
        setMessage({ type: 'error', text: err.response.data.message });
      } else {
        setMessage({ type: 'error', text: '100% 분석 요청에 100% 실패했습니다.' });
      }
    } finally {
      setIsLoading(false); 
    }
  };


  return (
    <aside className="sidebar">
      <h3>🔍 검색하기</h3>
      <form onSubmit={handleSearchSubmit} className="search-bar">
        <input
          type="text"
          className="search-input"
          placeholder="제목 또는 요약..."
          value={searchTerm}
          onChange={handleSearchChange}
        />
        <button type="submit" className="search-button">
          검색
        </button>
      </form>

      <h3>📚 주제별 분류</h3>
      <div className="filter-group">
        <ul>
          {/* 1. ★ 내 보관소 */}
          <li>
            <button
              className={`filter-button ${activeFilter === 'FAVORITES' ? 'active' : ''}`}
              onClick={() => handleFilterClick('FAVORITES')}
            >
              ★ 내 보관소
            </button>
          </li>

          <li>
            <button
              className={`filter-button ${activeFilter === 'On-Demand' ? 'active' : ''}`}
              onClick={() => handleFilterClick('On-Demand')}
            >
              🚀 내 분석 모아보기
            </button>
          </li>
          
          <li>
            <button
              className={`filter-button ${activeFilter === null ? 'active' : ''}`}
              onClick={() => handleFilterClick(null)}
            >
              ✨ 전체 보기
            </button>
          </li>
          
          {/* 4. 동적 토픽 목록 */}
          {topics.map((topic) => (
            <li key={topic}>
              <button
                className={`filter-button ${activeFilter === topic ? 'active' : ''}`}
                onClick={() => handleFilterClick(topic)}
              >
                {topic}
              </button>
            </li>
          ))}
        </ul>
      </div>

      <h3>🚀 즉시 분석하기</h3>
      <form onSubmit={handleAnalyzeSubmit} className="search-bar">
        <input
          type="text"
          className="search-input"
          placeholder="https://github.com/..."
          value={analyzeUrl}
          onChange={(e) => setAnalyzeUrl(e.target.value)}
          disabled={isLoading} 
        />
        <button 
          type="submit" 
          className="search-button" 
          disabled={isLoading} 
        >
          {isLoading ? '분석중...' : '분석'}
        </button>
      </form>
      {message && (
        <div className={`analyze-message ${message.type === 'error' ? 'error' : 'success'}`}>
          {message.text}
        </div>
      )}
    </aside>
  );
}

export default Sidebar;