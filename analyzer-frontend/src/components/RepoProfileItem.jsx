import React from 'react';
import '../styles/RepoProfileItem.css';

function RepoProfileItem({ profile, onToggleFavorite, isFavorite, onDelete }) {
  const { id, repoName, repoUrl, techStackSummary, imageUrl, projectTitle } = profile;

  if (!profile) return null;

  const handleFavoriteClick = (e) => {
    e.preventDefault();
    e.stopPropagation();
    onToggleFavorite(id);
  };

  const handleDeleteClick = (e) => {
    e.preventDefault();
    e.stopPropagation();
    onDelete(id);
  };

  return (
    <li className="repo-profile-item">
      {imageUrl && (
        <a href={repoUrl || '#'} target="_blank" rel="noopener noreferrer" className="repo-image-link">
          <div className="repo-image-container">
            <img src={imageUrl} alt={repoName} className="repo-image" />
          </div>
        </a>
      )}

      <div className="repo-content">
        <h3>
          <a href={repoUrl || '#'} target="_blank" rel="noopener noreferrer">
            {projectTitle || repoName}
          </a>
        </h3>
        {techStackSummary && (
          <p className="tech-summary">
            <strong>기술 스택 요약:</strong> {techStackSummary}
          </p>
        )}
      </div>

      {/* 즐겨찾기 버튼 */}
      <button
        className={`favorite-button ${isFavorite ? 'favorited' : ''}`}
        onClick={handleFavoriteClick}
        title={isFavorite ? '보관소에서 제거' : '보관소에 추가'}
      >
        {isFavorite ? '★' : '☆'}
      </button>
      
      {/* 삭제 버튼 */}
      <button
        className="delete-button"
        onClick={handleDeleteClick}
        title="분석 결과 삭제"
      >
        🗑️
      </button>

    </li>
  );
}

export default RepoProfileItem;