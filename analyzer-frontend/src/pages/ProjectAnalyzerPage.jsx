import React, { useState, useEffect } from 'react';
import axios from 'axios';
import MainLayout from '../components/MainLayout.jsx';
import Sidebar from '../components/Sidebar.jsx';
import RepoProfileList from '../components/RepoProfileList.jsx';

const FAVORITES_STORAGE_KEY = 'project_analyzer_favorites';

function ProjectAnalyzerPage() {
  const [searchKeyword, setSearchKeyword] = useState('');
  const [filterTopic, setFilterTopic] = useState(null);
  const [favorites, setFavorites] = useState(new Set());

  // 즐겨찾기 불러오기
  useEffect(() => {
    try {
      const storedFavorites = localStorage.getItem(FAVORITES_STORAGE_KEY);
      if (storedFavorites) {
        setFavorites(new Set(JSON.parse(storedFavorites)));
      }
    } catch (err) {
      console.error("즐겨찾기 로딩 실패:", err);
    }
  }, []);

  // 즐겨찾기 토글
  const toggleFavorite = (profileId) => {
    const newFavorites = new Set(favorites);
    if (newFavorites.has(profileId)) {
      newFavorites.delete(profileId);
    } else {
      newFavorites.add(profileId);
    }
    setFavorites(newFavorites);
    try {
      localStorage.setItem(FAVORITES_STORAGE_KEY, JSON.stringify(Array.from(newFavorites)));
    } catch (err) {
      console.error("즐겨찾기 저장 실패:", err);
    }
  };

  const handleSearch = (keyword) => {
    setSearchKeyword(keyword);
    setFilterTopic(null);
  };

  const handleFilter = (topic) => {
    setFilterTopic(topic);
    setSearchKeyword('');
  };

  // 삭제 처리
  const handleDelete = async (profileId) => {
    if (!window.confirm("정말 삭제하시겠습니까?")) return;
    
    try {
      await axios.delete(`http://localhost:8080/api/projects/${profileId}`);
      if (favorites.has(profileId)) {
        toggleFavorite(profileId);
      }
    } catch (err) {
      console.error("삭제 실패:", err);
      alert("삭제 중 오류가 발생했습니다.");
    }
  };

  return (
    <MainLayout
      sidebar={
        <Sidebar 
          onSearch={handleSearch} 
          onFilter={handleFilter} 
          activeFilter={filterTopic} 
        />
      }
      content={
        <RepoProfileList
          searchKeyword={searchKeyword}
          filterTopic={filterTopic}
          favorites={favorites}
          onToggleFavorite={toggleFavorite}
          onDelete={handleDelete}
        />
      }
    />
  );
}

export default ProjectAnalyzerPage;