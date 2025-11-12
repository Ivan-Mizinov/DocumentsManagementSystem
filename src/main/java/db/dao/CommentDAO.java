package db.dao;

import db.entities.Comment;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class CommentDAO extends BaseDAO<Comment> {
    public CommentDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public List<Comment> getCommentsByPageVersionId(Long pageVersionId) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM Comment c WHERE c.pageVersion.id = :pageVersionId ORDER BY c.createdAt",
                            Comment.class)
                    .setParameter("pageVersionId", pageVersionId)
                    .list();
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
}
