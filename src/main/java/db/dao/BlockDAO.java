package db.dao;

import db.entities.Block;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;

public class BlockDAO {
    private SessionFactory sessionFactory;

    public BlockDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Block findById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            return session.find(Block.class, id);
        }
    }

    public List<Block> getAllBlocks() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM Block", Block.class).list();
        }
    }

    public void save(Block block) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            session.persist(block);
            transaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void update(Block block) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            session.merge(block);
            transaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void delete(Block block) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            session.remove(block);
            transaction.commit();
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
