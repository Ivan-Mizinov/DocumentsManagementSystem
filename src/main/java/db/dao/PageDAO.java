package db.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import db.dto.HeadingDTO;
import db.dto.PageDTO;
import db.entities.Heading;
import db.entities.Page;
import db.util.RedisCacheUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.stream.Collectors;

public class PageDAO extends BaseDAO<Page, PageDTO> {
    private static final String ALL_PAGES_KEY = "page:all";
    private static final String SLUG_KEY_TEMPLATE = "page:slug:%s";
    private static final String HEADINGS_KEY_TEMPLATE = "page:%d:headings";
    private static final TypeReference<List<PageDTO>> PAGE_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<HeadingDTO>> HEADING_LIST_TYPE = new TypeReference<>() {
    };

    public PageDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    protected PageDTO entityToDTO(Page entity) {
        if (entity == null) return null;
        return new PageDTO(
                entity.getId(),
                entity.getTitle(),
                entity.getSlug(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    @Override
    protected Page dtoToEntity(PageDTO dto) {
        if (dto == null) return null;
        Page page = new Page();
        page.setId(dto.getId());
        page.setTitle(dto.getTitle());
        page.setSlug(dto.getSlug());
        page.setCreatedAt(dto.getCreatedAt());
        page.setUpdatedAt(dto.getUpdatedAt());
        return page;
    }

    @Override
    protected Class<PageDTO> getDTOClass() {
        return PageDTO.class;
    }

    public List<Page> findAll() {
        List<PageDTO> cachedDTOs = RedisCacheUtil.getValue(ALL_PAGES_KEY, PAGE_LIST_TYPE);
        if (cachedDTOs != null) {
            return cachedDTOs.stream().map(this::dtoToEntity).collect(Collectors.toList());
        }
        try (Session session = getSession()) {
            List<Page> pages = session.createQuery("FROM Page", Page.class).list();
            List<PageDTO> DTOs = pages.stream().map(this::entityToDTO).collect(Collectors.toList());
            RedisCacheUtil.cacheValue(ALL_PAGES_KEY, DTOs);
            return pages;
        }
    }

    public Page findBySlug(String slug) {
        String key = slugKey(slug);
        PageDTO cachedDTO = RedisCacheUtil.getValue(key, PageDTO.class);
        if (cachedDTO != null) {
            return dtoToEntity(cachedDTO);
        }
        try (Session session = getSession()) {
            Page page = session.createQuery("FROM Page p WHERE p.slug = :slug", Page.class)
                    .setParameter("slug", slug)
                    .uniqueResult();
            if (page != null) {
                RedisCacheUtil.cacheValue(key, entityToDTO(page));
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
        RedisCacheUtil.cacheValue(slugKey(saved.getSlug()), entityToDTO(saved));
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
        RedisCacheUtil.cacheValue(slugKey(updated.getSlug()), entityToDTO(updated));
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
        List<HeadingDTO> cachedDTOs = RedisCacheUtil.getValue(key, HEADING_LIST_TYPE);
        if (cachedDTOs != null) {
            return cachedDTOs.stream().map(this::headingDTOToEntity).collect(Collectors.toList());
        }
        try (Session session = getSession()) {
            List<Heading> headings = session.createQuery(
                            "FROM Heading h WHERE h.page.id = :pageId ORDER BY h.position",
                            Heading.class)
                    .setParameter("pageId", pageId)
                    .list();
            List<HeadingDTO> DTOs = headings.stream().map(this::headingEntityToDTO).collect(Collectors.toList());
            RedisCacheUtil.cacheValue(key, DTOs);
            return headings;
        }
    }

    private HeadingDTO headingEntityToDTO(Heading entity) {
        if (entity == null) return null;
        Long pageId = entity.getPage() != null ? entity.getPage().getId() : null;
        return new HeadingDTO(
                entity.getId(),
                pageId,
                entity.getLevel(),
                entity.getText(),
                entity.getPosition()
        );
    }

    private Heading headingDTOToEntity(HeadingDTO dto) {
        if (dto == null) return null;
        Heading heading = new Heading();
        heading.setId(dto.getId());
        heading.setLevel(dto.getLevel());
        heading.setText(dto.getText());
        heading.setPosition(dto.getPosition());
        // Загружаем Page по ID из базы
        if (dto.getPageId() != null) {
            try (Session session = getSession()) {
                Page page = session.find(Page.class, dto.getPageId());
                heading.setPage(page);
            }
        }
        return heading;
    }

    private String slugKey(String slug) {
        return String.format(SLUG_KEY_TEMPLATE, slug);
    }

    private String headingsKey(Long pageId) {
        return String.format(HEADINGS_KEY_TEMPLATE, pageId);
    }
}
