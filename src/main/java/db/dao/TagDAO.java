package db.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import db.dto.TagDTO;
import db.entities.Page;
import db.entities.Tag;
import db.util.RedisCacheUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TagDAO extends BaseDAO<Tag, TagDTO> {
    private static final String ALL_TAGS_KEY = "tag:all";
    private static final String TAGS_BY_PAGE_KEY_TEMPLATE = "tag:page:%d";
    private static final String PAGES_BY_TAG_KEY_TEMPLATE = "tag:name:%s:pages";
    private static final TypeReference<List<TagDTO>> TAG_LIST_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<Page>> PAGE_LIST_TYPE = new TypeReference<>() {};

    public TagDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    protected TagDTO entityToDTO(Tag entity) {
        if (entity == null) return null;
        return new TagDTO(entity.getId(), entity.getName(), entity.getDescription());
    }

    @Override
    protected Tag dtoToEntity(TagDTO dto) {
        if (dto == null) return null;
        Tag tag = new Tag();
        tag.setId(dto.getId());
        tag.setName(dto.getName());
        tag.setDescription(dto.getDescription());
        return tag;
    }

    @Override
    protected Class<TagDTO> getDTOClass() {
        return TagDTO.class;
    }

    @Override
    public Tag save(Tag tag) {
        if (tag.getName() != null) {
            Tag existingTag = findByName(tag.getName());
            if (existingTag != null) {
                return existingTag;
            }
        }
        Tag saved = super.save(tag);
        evictTagCaches(saved);
        return saved;
    }

    private Tag findByName(String name) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "SELECT t FROM Tag t WHERE t.name = :name", Tag.class)
                    .setParameter("name", name)
                    .uniqueResult();
        } catch (Exception e) {
            System.err.println("Ошибка при поиске тега по имени '" + name + "': " + e.getMessage());
            return null;
        }
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
        List<TagDTO> cachedDTOs = RedisCacheUtil.getValue(ALL_TAGS_KEY, TAG_LIST_TYPE);
        if (cachedDTOs != null) {
            return cachedDTOs.stream().map(this::dtoToEntity).collect(Collectors.toList());
        }
        try (Session session = getSession()) {
            List<Tag> tags = session.createQuery("FROM Tag", Tag.class).list();
            List<TagDTO> DTOs = tags.stream().map(this::entityToDTO).collect(Collectors.toList());
            RedisCacheUtil.cacheValue(ALL_TAGS_KEY, DTOs);
            return tags;
        }
    }

    public List<Tag> getTagsByPageId(Long pageId) {
        String key = tagsByPageKey(pageId);
        List<TagDTO> cachedDTOs = RedisCacheUtil.getValue(key, TAG_LIST_TYPE);
        if (cachedDTOs != null) {
            return cachedDTOs.stream().map(this::dtoToEntity).collect(Collectors.toList());
        }
        try (Session session = getSession()) {
            List<Tag> tags = session.createQuery(
                            "SELECT t FROM Tag t JOIN t.pages p WHERE p.id = :pageId",
                            Tag.class)
                    .setParameter("pageId", pageId)
                    .list();
            List<TagDTO> DTOs = tags.stream().map(this::entityToDTO).collect(Collectors.toList());
            RedisCacheUtil.cacheValue(key, DTOs);
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
