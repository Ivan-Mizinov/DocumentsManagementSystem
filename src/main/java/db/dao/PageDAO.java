package db.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import db.entities.Heading;
import db.entities.Page;
import db.util.RedisCacheUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class PageDAO extends BaseDAO<Page> {
    private static final String ALL_PAGES_KEY = "page:all";
    private static final String SLUG_KEY_TEMPLATE = "page:slug:%s";
    private static final String HEADINGS_KEY_TEMPLATE = "page:%d:headings";
    private static final TypeReference<List<Page>> PAGE_LIST_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<Heading>> HEADING_LIST_TYPE = new TypeReference<>() {};

    public PageDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public List<Page> findAll() {
        List<Page> cached = RedisCacheUtil.getValue(ALL_PAGES_KEY, PAGE_LIST_TYPE);
        if (cached != null) {
            return cached;
        }
        try (Session session = getSession()) {
            List<Page> pages = session.createQuery("FROM Page", Page.class).list();
            RedisCacheUtil.cacheValue(ALL_PAGES_KEY, pages);
            return pages;
        }
    }

    public Page findBySlug(String slug) {
        String key = slugKey(slug);
        Page cached = RedisCacheUtil.getValue(key, Page.class);
        if (cached != null) {
            return cached;
        }
        try (Session session = getSession()) {
            Page page = session.createQuery("FROM Page p WHERE p.slug = :slug", Page.class)
                    .setParameter("slug", slug)
                    .uniqueResult();
            if (page != null) {
                RedisCacheUtil.cacheValue(key, page);
                cacheEntity(page);
            }
            return page;
        }
    }

    @Override
    public Page save(Page page) {
        String slug = page.getSlug();
        Page existingPage = findBySlug(slug);
        if (existingPage != null) {
            System.out.println("Страница с '" + slug + "' уже существует. Возвращаем существующую запись.");
            cacheEntity(page);
            return existingPage;
        }
        Page saved = super.save(page);
        RedisCacheUtil.evict(ALL_PAGES_KEY);
        RedisCacheUtil.cacheValue(slugKey(saved.getSlug()), saved);
        return saved;
    }

    @Override
    public Page update(Page page) {
        Page persisted = findById(Page.class, page.getId());
        String previousSlug = persisted != null ? persisted.getSlug() : null;
        Page updated = super.update(page);
        RedisCacheUtil.evict(ALL_PAGES_KEY);
        if (previousSlug != null) {
            RedisCacheUtil.evict(slugKey(previousSlug));
        }
        RedisCacheUtil.cacheValue(slugKey(updated.getSlug()), updated);
        RedisCacheUtil.evict(headingsKey(updated.getId()));
        return updated;
    }

    @Override
    public void delete(Page page) {
        String slug = page.getSlug();
        Long pageId = page.getId();
        super.delete(page);
        RedisCacheUtil.evict(ALL_PAGES_KEY);
        if (slug != null) {
            RedisCacheUtil.evict(slugKey(slug));
        }
        if (pageId != null) {
            RedisCacheUtil.evict(headingsKey(pageId));
        }
    }

    public List<Heading> getHeadingsByPageId(Long pageId) {
        String key = headingsKey(pageId);
        List<Heading> cached = RedisCacheUtil.getValue(key, HEADING_LIST_TYPE);
        if (cached != null) {
            return cached;
        }
        try (Session session = getSession()) {
            List<Heading> headings = session.createQuery(
                            "FROM Heading h WHERE h.page.id = :pageId ORDER BY h.position",
                            Heading.class)
                    .setParameter("pageId", pageId)
                    .list();
            RedisCacheUtil.cacheValue(key, headings);
            return headings;
        }
    }

    private String slugKey(String slug) {
        return String.format(SLUG_KEY_TEMPLATE, slug);
    }

    private String headingsKey(Long pageId) {
        return String.format(HEADINGS_KEY_TEMPLATE, pageId);
    }
}
