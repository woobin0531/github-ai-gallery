import React, { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import RepoProfileItem from './RepoProfileItem.jsx';
import '../styles/RepoProfileList.css';

const SORT_OPTIONS = [
  { value: 'createdAt,desc', label: '최신 분석순' },
];
const PAGE_SIZE = 12;
const API_BASE_URL = 'http://localhost:8080/api/projects';

function RepoProfileList({ searchKeyword, filterTopic, favorites, onToggleFavorite, onDelete, onRegenerate }) {
  const [profiles, setProfiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [currentSort, setCurrentSort] = useState(SORT_OPTIONS[0].value);

  // 데이터 가져오기 (검색, 필터, 즐겨찾기 분기 처리)
  const fetchProfiles = useCallback(async () => {
    setLoading(true);
    setError(null);

    let apiUrl = API_BASE_URL;
    let params = { page: currentPage, size: PAGE_SIZE, sort: currentSort };
    let httpMethod = 'get';
    let requestBody = null;

    // 1. 검색어 모드
    if (searchKeyword && searchKeyword.trim() !== '') {
      apiUrl = `${API_BASE_URL}/search`;
      params.keyword = searchKeyword;
    
    // 2. 내 보관소(즐겨찾기) 모드
    } else if (filterTopic === 'FAVORITES') {
      const favoriteIds = Array.from(favorites);
      if (favoriteIds.length === 0) {
        setProfiles([]);
        setTotalPages(0);
        setLoading(false);
        return;
      }
      apiUrl = `${API_BASE_URL}/favorites`;
      httpMethod = 'post';
      requestBody = favoriteIds;
    
    // 3. 주제 필터 모드
    } else if (filterTopic) {
      apiUrl = `${API_BASE_URL}/filter`;
      params.topic = filterTopic;
    }

    try {
      let response;
      if (httpMethod === 'post') {
        response = await axios.post(apiUrl, requestBody, { params });
      } else {
        response = await axios.get(apiUrl, { params });
      }
      setProfiles(response.data.content || []);
      setTotalPages(response.data.totalPages || 0);
    } catch (err) {
      console.error("API Error:", err);
      setError("데이터를 불러오는 중 오류가 발생했습니다.");
      setProfiles([]);
      setTotalPages(0);
    } finally {
      setLoading(false);
    }
  }, [currentPage, currentSort, searchKeyword, filterTopic, favorites]);

  // 조건 변경 시 데이터 재요청
  useEffect(() => {
    fetchProfiles();
  }, [fetchProfiles]);

  // 검색어나 필터가 바뀌면 1페이지로 초기화
  useEffect(() => {
    setCurrentPage(0);
  }, [searchKeyword, filterTopic]);

  // 삭제 시 화면 즉시 갱신
  const handleDeleteProfile = async (profileId) => {
    await onDelete(profileId);
    setProfiles((current) => current.filter((p) => p.id !== profileId));
  };

  const handleSortChange = (e) => {
    setCurrentSort(e.target.value);
    setCurrentPage(0);
  };

  const handlePageChange = (newPage) => {
    if (newPage >= 0 && newPage < totalPages) {
      setCurrentPage(newPage);
    }
  };

  // 결과 없음 메시지 생성
  const getEmptyMessage = () => {
    if (searchKeyword) return `"${searchKeyword}"에 대한 검색 결과가 없습니다.`;
    if (filterTopic === 'FAVORITES') return '★ 보관소에 저장된 항목이 없습니다.';
    if (filterTopic) return `"${filterTopic}" 주제의 결과가 없습니다.`;
    return '저장소 정보가 없습니다.';
  };

  return (
    <div>
      {/* 정렬 옵션 */}
      <div className="sort-bar">
        <div className="sort-options">
          <label htmlFor="sort-select">정렬:</label>
          <select id="sort-select" value={currentSort} onChange={handleSortChange} className="sort-select">
            {SORT_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
        </div>
      </div>

      {/* 로딩 및 에러 */}
      {loading && <div className="loading-container"><div className="spinner"></div></div>}
      {error && <p className="error-message">{error}</p>}

      {/* 프로필 리스트 */}
      {!loading && !error && (
        <>
          <ul className="repo-profile-list">
            {profiles.length > 0 ? (
              profiles.map((profile) => (
                <RepoProfileItem
                  key={profile.id}
                  profile={profile}
                  onToggleFavorite={onToggleFavorite}
                  isFavorite={favorites.has(profile.id)}
                  onDelete={handleDeleteProfile}
                  onRegenerate={onRegenerate}
                />
              ))
            ) : (
              <p className="no-results-message">{getEmptyMessage()}</p>
            )}
          </ul>

          {/* 페이지네이션 */}
          {totalPages > 1 && (
            <div className="pagination">
              <button 
                onClick={() => handlePageChange(currentPage - 1)} 
                disabled={currentPage === 0} 
                className="page-button"
              >
                이전
              </button>
              <span className="page-info">페이지 {currentPage + 1} / {totalPages}</span>
              <button 
                onClick={() => handlePageChange(currentPage + 1)} 
                disabled={currentPage === totalPages - 1} 
                className="page-button"
              >
                다음
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

export default RepoProfileList;