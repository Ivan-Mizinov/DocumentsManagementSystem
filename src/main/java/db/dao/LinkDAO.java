package db.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import db.entities.Link;
import db.util.RedisCacheUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class LinkDAO extends BaseDAO<Link> {
    private static final String LINKS_BY_PAGE_KEY_TEMPLATE = "page:%d:links";
    private static final TypeReference<List<Link>> LINK_LIST_TYPE = new TypeReference<>() {};

    public LinkDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public Link save(Link link) {
        Link saved = super.save(link);
        evictLinksByPage(saved);
        return saved;
    }

    @Override
    public Link update(Link link) {
        Link updated = super.update(link);
        evictLinksByPage(updated);
        return updated;
    }

    @Override
    public void delete(Link link) {
        Long pageId = link.getPage() != null ? link.getPage().getId() : null;
        super.delete(link);
        if (pageId != null) {
            RedisCacheUtil.evict(linksKey(pageId));
        }
    }

    public List<Link> getLinksByPageId(Long pageId) {
        String key = linksKey(pageId);
        List<Link> cached = RedisCacheUtil.getValue(key, LINK_LIST_TYPE);
        if (cached != null) {
            return cached;
        }
        try (Session session = getSession()) {
            List<Link> links = session.createQuery(
                            "FROM Link l WHERE l.page.id = :pageId",
                            Link.class)
                    .setParameter("pageId", pageId)
                    .list();
            RedisCacheUtil.cacheValue(key, links);
            return links;
        }
    }

    private void evictLinksByPage(Link link) {
        if (link != null && link.getPage() != null && link.getPage().getId() != null) {
            RedisCacheUtil.evict(linksKey(link.getPage().getId()));
        }
    }

    private String linksKey(Long pageId) {
        return String.format(LINKS_BY_PAGE_KEY_TEMPLATE, pageId);
    }
}
