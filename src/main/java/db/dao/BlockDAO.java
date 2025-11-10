package db.dao;

import db.entities.Block;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class BlockDAO extends BaseDAO<Block> {
    public BlockDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public List<Block> getAllBlocks() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM Block", Block.class).list();
        }
    }

    public List<Block> getBlocksByPageId(Long pageId) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                            "FROM Block b WHERE b.page.id = :pageId", Block.class)
                    .setParameter("pageId", pageId)
                    .list();
        }
    }
}
