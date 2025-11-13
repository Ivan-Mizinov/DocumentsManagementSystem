package db.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import db.entities.Comment;
import db.util.RedisCacheUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class CommentDAO extends BaseDAO<Comment> {
    private static final String COMMENTS_BY_VERSION_KEY_TEMPLATE = "comments:version:%d";
    private static final TypeReference<List<Comment>> COMMENT_LIST_TYPE = new TypeReference<>() {};

    public CommentDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
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
        List<Comment> cached = RedisCacheUtil.getValue(key, COMMENT_LIST_TYPE);
        if (cached != null) {
            return cached;
        }
        try (Session session = getSession()) {
            List<Comment> comments = session.createQuery(
                            "FROM Comment c WHERE c.pageVersion.id = :pageVersionId ORDER BY c.createdAt",
                            Comment.class)
                    .setParameter("pageVersionId", pageVersionId)
                    .list();
            RedisCacheUtil.cacheValue(key, comments);
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
