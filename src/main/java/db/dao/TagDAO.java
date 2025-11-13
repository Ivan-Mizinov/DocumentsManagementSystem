package db.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import db.entities.Page;
import db.entities.Tag;
import db.util.RedisCacheUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TagDAO extends BaseDAO<Tag> {
    private static final String ALL_TAGS_KEY = "tag:all";
    private static final String TAGS_BY_PAGE_KEY_TEMPLATE = "tag:page:%d";
    private static final String PAGES_BY_TAG_KEY_TEMPLATE = "tag:name:%s:pages";
    private static final TypeReference<List<Tag>> TAG_LIST_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<Page>> PAGE_LIST_TYPE = new TypeReference<>() {};

    public TagDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public Tag save(Tag tag) {
        Tag saved = super.save(tag);
        evictTagCaches(saved);
        return saved;
    }

    @Override
    public Tag update(Tag tag) {
        Tag updated = super.update(tag);
        evictTagCaches(updated);
        return updated;
    }

    @Override
    public void delete(Tag tag) {
        Set<Page> relatedPages = tag.getPages() != null ? new HashSet<>(tag.getPages()) : Set.of();
        String name = tag.getName();
        super.delete(tag);
        RedisCacheUtil.evict(ALL_TAGS_KEY);
        if (name != null) {
            RedisCacheUtil.evict(pagesByTagKey(name));
        }
        for (Page page : relatedPages) {
            if (page != null && page.getId() != null) {
                RedisCacheUtil.evict(tagsByPageKey(page.getId()));
            }
        }
    }

    public List<Tag> getAllTags() {
        List<Tag> cached = RedisCacheUtil.getValue(ALL_TAGS_KEY, TAG_LIST_TYPE);
        if (cached != null) {
            return cached;
        }
        try (Session session = getSession()) {
            List<Tag> tags = session.createQuery("FROM Tag", Tag.class).list();
            RedisCacheUtil.cacheValue(ALL_TAGS_KEY, tags);
            return tags;
        }
    }

    public List<Tag> getTagsByPageId(Long pageId) {
        String key = tagsByPageKey(pageId);
        List<Tag> cached = RedisCacheUtil.getValue(key, TAG_LIST_TYPE);
        if (cached != null) {
            return cached;
        }
        try (Session session = getSession()) {
            List<Tag> tags = session.createQuery(
                            "SELECT t FROM Tag t JOIN t.pages p WHERE p.id = :pageId",
                            Tag.class)
                    .setParameter("pageId", pageId)
                    .list();
            RedisCacheUtil.cacheValue(key, tags);
            return tags;
        } catch (Exception e) {
            System.out.println("Ошибка при получении тегов для pageId=" + pageId + ": " + e.getMessage());
            return null;
        }
    }

    public List<Page> findPagesByTag(String tagName) {
        String key = pagesByTagKey(tagName);
        List<Page> cached = RedisCacheUtil.getValue(key, PAGE_LIST_TYPE);
        if (cached != null) {
            return cached;
        }
        try (Session session = getSession()) {
            List<Page> pages = session.createQuery(
                            "SELECT p FROM Page p JOIN p.tags t WHERE t.name = :tagName",
                            Page.class)
                    .setParameter("tagName", tagName)
                    .list();
            RedisCacheUtil.cacheValue(key, pages);
            return pages;
        } catch (Exception e) {
            System.out.println("Ошибка при поиске страниц по тегу '" + tagName + "': " + e.getMessage());
            return null;
        }
    }

    private void evictTagCaches(Tag tag) {
        RedisCacheUtil.evict(ALL_TAGS_KEY);
        if (tag.getName() != null) {
            RedisCacheUtil.evict(pagesByTagKey(tag.getName()));
        }
        if (tag.getPages() != null) {
            for (Page page : tag.getPages()) {
                if (page != null && page.getId() != null) {
                    RedisCacheUtil.evict(tagsByPageKey(page.getId()));
                }
            }
        }
    }

    private String tagsByPageKey(Long pageId) {
        return String.format(TAGS_BY_PAGE_KEY_TEMPLATE, pageId);
    }

    private String pagesByTagKey(String tagName) {
        return String.format(PAGES_BY_TAG_KEY_TEMPLATE, tagName);
    }
}
