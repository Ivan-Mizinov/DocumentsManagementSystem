package db.dao;

import db.entities.Link;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class LinkDAO extends BaseDAO<Link> {
    public LinkDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public List<Link> getLinksByPageId(Long pageId) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM Link l WHERE l.page.id = :pageId",
                            Link.class)
                    .setParameter("pageId", pageId)
                    .list();
        }
    }
}
