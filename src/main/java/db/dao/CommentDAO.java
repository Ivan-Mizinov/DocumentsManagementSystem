package db.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import db.dto.CommentDTO;
import db.entities.Comment;
import db.entities.PageVersion;
import db.entities.User;
import db.util.RedisCacheUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.stream.Collectors;

public class CommentDAO extends BaseDAO<Comment, CommentDTO> {
    private static final String COMMENTS_BY_VERSION_KEY_TEMPLATE = "comments:version:%d";
    private static final TypeReference<List<CommentDTO>> COMMENT_LIST_TYPE = new TypeReference<>() {
    };

    public CommentDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    protected CommentDTO entityToDTO(Comment entity) {
        if (entity == null) return null;
        Long pageVersionId = entity.getPageVersion() != null ? entity.getPageVersion().getId() : null;
        Long authorId = entity.getAuthor() != null ? entity.getAuthor().getId() : null;
        return new CommentDTO(
                entity.getId(),
                pageVersionId,
                authorId,
                entity.getText(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.isResolved()
        );
    }

    @Override
    protected Comment dtoToEntity(CommentDTO dto) {
        if (dto == null) return null;
        Comment comment = new Comment();
        comment.setId(dto.getId());
        comment.setText(dto.getText());
        comment.setCreatedAt(dto.getCreatedAt());
        comment.setUpdatedAt(dto.getUpdatedAt());
        comment.setResolved(dto.isResolved());

        try (Session session = getSession()) {
            if (dto.getPageVersionId() != null) {
                PageVersion pageVersion = session.find(PageVersion.class, dto.getPageVersionId());
                comment.setPageVersion(pageVersion);
            }
            if (dto.getAuthorId() != null) {
                User author = session.find(User.class, dto.getAuthorId());
                comment.setAuthor(author);
            }
        }
        return comment;
    }

    @Override
    protected Class<CommentDTO> getDTOClass() {
        return CommentDTO.class;
    }

    @Override
    public Comment save(Comment comment) {
        Comment saved = super.save(comment);
        evictByVersion(saved);
        return saved;
    }

    @Override
    public Comment update(Comment comment) {
        Comment updated = super.update(comment);
        evictByVersion(updated);
        return updated;
    }

    @Override
    public void delete(Comment comment) {
        Long pageVersionId = comment.getPageVersion() != null ? comment.getPageVersion().getId() : null;
        super.delete(comment);
        if (pageVersionId != null) {
            RedisCacheUtil.evict(versionKey(pageVersionId));
        }
    }

    public List<Comment> getCommentsByPageVersionId(Long pageVersionId) {
        String key = versionKey(pageVersionId);
        List<CommentDTO> cachedDTOs = RedisCacheUtil.getValue(key, COMMENT_LIST_TYPE);
        if (cachedDTOs != null) {
            return cachedDTOs.stream().map(this::dtoToEntity).collect(Collectors.toList());
        }
        try (Session session = getSession()) {
            List<Comment> comments = session.createQuery(
                            "FROM Comment c WHERE c.pageVersion.id = :pageVersionId ORDER BY c.createdAt",
                            Comment.class)
                    .setParameter("pageVersionId", pageVersionId)
                    .list();
            List<CommentDTO> DTOs = comments.stream().map(this::entityToDTO).collect(Collectors.toList());
            RedisCacheUtil.cacheValue(key, DTOs);
            return comments;
        }
    }

    public List<Comment> getCommentsByPageId(Long pageId) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM Comment c WHERE c.pageVersion.page.id = :pageId ORDER BY c.createdAt",
                            Comment.class)
                    .setParameter("pageId", pageId)
                    .list();
        }
    }

    private void evictByVersion(Comment comment) {
        if (comment != null && comment.getPageVersion() != null && comment.getPageVersion().getId() != null) {
            RedisCacheUtil.evict(versionKey(comment.getPageVersion().getId()));
        }
    }

    private String versionKey(Long pageVersionId) {
        return String.format(COMMENTS_BY_VERSION_KEY_TEMPLATE, pageVersionId);
    }
}
