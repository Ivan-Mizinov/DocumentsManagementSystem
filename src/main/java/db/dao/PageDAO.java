package db.dao;

import db.entities.Heading;
import db.entities.Page;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class PageDAO extends BaseDAO<Page> {
    public PageDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public List<Page> findAll() {
        try (Session session = getSession()) {
            return session.createQuery("FROM Page", Page.class).list();
        }
    }

    public Page findBySlug(String slug) {
        try (Session session = getSession()) {
            return session.createQuery("FROM Page p WHERE p.slug = :slug", Page.class)
                    .setParameter("slug", slug)
                    .uniqueResult();
        }
    }

    @Override
    public Page save(Page page) {
        String slug = page.getSlug();
        Page existingPage = findBySlug(slug);
        if (existingPage != null) {
            System.out.println("Страница с '" + slug + "' уже существует. Возвращаем существующую запись.");
            return existingPage;
        }
        return super.save(page);
    }

    public List<Heading> getHeadingsByPageId(Long pageId) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM Heading h WHERE h.page.id = :pageId ORDER BY h.position",
                            Heading.class)
                    .setParameter("pageId", pageId)
                    .list();
        }
    }
}
