package db.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import db.dto.PageVersionDTO;
import db.entities.Page;
import db.entities.PageVersion;
import db.entities.User;
import db.util.RedisCacheUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class PageVersionDAO {
    private static final String LATEST_VERSION_KEY_TEMPLATE = "page:%d:version:latest";
    private static final String PAGE_VERSIONS_KEY_TEMPLATE = "page:%d:versions";
    private static final String VERSION_BY_ID_KEY_TEMPLATE = "pageversion:id:%d";
    private static final TypeReference<List<PageVersionDTO>> PAGE_VERSION_LIST_TYPE = new TypeReference<>() {};

    private final SessionFactory sessionFactory;

    public PageVersionDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private PageVersionDTO entityToDTO(PageVersion entity) {
        if (entity == null) return null;
        Long pageId = entity.getPage() != null ? entity.getPage().getId() : null;
        Long changedById = entity.getChangedBy() != null ? entity.getChangedBy().getId() : null;
        return new PageVersionDTO(
                entity.getId(),
                pageId,
                entity.getVersionNumber(),
                entity.getContent(),
                changedById,
                entity.getChangedAt(),
                entity.isPublished()
        );
    }

    private PageVersion dtoToEntity(PageVersionDTO dto) {
        if (dto == null) return null;
        PageVersion version = new PageVersion();
        version.setId(dto.getId());
        version.setVersionNumber(dto.getVersionNumber());
        version.setContent(dto.getContent());
        version.setChangedAt(dto.getChangedAt());
        version.setPublished(dto.isPublished());

        try (Session session = sessionFactory.openSession()) {
            if (dto.getPageId() != null) {
                Page page = session.find(Page.class, dto.getPageId());
                version.setPage(page);
            }
            if (dto.getChangedById() != null) {
                User changedBy = session.find(User.class, dto.getChangedById());
                version.setChangedBy(changedBy);
            }
        }
        return version;
    }

    public PageVersion findLatestVersion(Long pageId) {
        String key = latestVersionKey(pageId);
        PageVersionDTO cachedDTO = RedisCacheUtil.getValue(key, PageVersionDTO.class);
        if (cachedDTO != null) {
            return dtoToEntity(cachedDTO);
        }
        try (Session session = sessionFactory.openSession()) {
            PageVersion version = session.createQuery(
                            "FROM PageVersion v WHERE v.page.id = :pageId ORDER BY v.versionNumber DESC", PageVersion.class)
                    .setParameter("pageId", pageId)
                    .setMaxResults(1)
                    .uniqueResult();
            if (version != null) {
                cacheVersion(version);
                RedisCacheUtil.cacheValue(key, entityToDTO(version));
            }
            return version;
        }
    }

    public PageVersion createNewVersion(Page page, User changer, String newContent) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            Integer latestVersionNumber = session.createQuery(
                            "SELECT max(v.versionNumber) FROM PageVersion v WHERE v.page.id = :pageId",
                            Integer.class)
                    .setParameter("pageId", page.getId())
                    .uniqueResult();
            int nextVersion = latestVersionNumber == null ? 1 : latestVersionNumber + 1;

            Page managedPage = session.merge(page);

            PageVersion version = new PageVersion();
            version.setPage(managedPage);
            version.setVersionNumber(nextVersion);
            version.setContent(newContent);
            version.setChangedBy(changer);
            version.setChangedAt(LocalDateTime.now());
            version.setPublished(false);

            session.persist(version);
            transaction.commit();

            cacheVersion(version);
            RedisCacheUtil.cacheValue(latestVersionKey(page.getId()), entityToDTO(version));
            RedisCacheUtil.evict(latestVersionKey(page.getId()));

            return version;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<PageVersion> findAllVersions(Long pageId) {
        String key = pageVersionsKey(pageId);
        List<PageVersionDTO> cachedDTOs = RedisCacheUtil.getValue(key, PAGE_VERSION_LIST_TYPE);
        if (cachedDTOs != null) {
            return cachedDTOs.stream().map(this::dtoToEntity).collect(Collectors.toList());
        }
        try (Session session = sessionFactory.openSession()) {
            List<PageVersion> versions = session.createQuery(
                            "FROM PageVersion v WHERE v.page.id = :pageId ORDER BY v.versionNumber", PageVersion.class)
                    .setParameter("pageId", pageId)
                    .list();
            List<PageVersionDTO> DTOs = versions.stream().map(this::entityToDTO).collect(Collectors.toList());
            RedisCacheUtil.cacheValue(key, DTOs);
            return versions;
        }
    }

    public PageVersion findById(Long id) {
        String key = versionByIdKey(id);
        PageVersionDTO cachedDTO = RedisCacheUtil.getValue(key, PageVersionDTO.class);
        if (cachedDTO != null) {
            return dtoToEntity(cachedDTO);
        }
        try (Session session = sessionFactory.openSession()) {
            PageVersion version = session.find(PageVersion.class, id);
            if (version != null) {
                cacheVersion(version);
            }
            return version;
        }
    }

    private void cacheVersion(PageVersion version) {
        if (version != null && version.getId() != null) {
            RedisCacheUtil.cacheValue(versionByIdKey(version.getId()), entityToDTO(version));
        }
    }

    private String latestVersionKey(Long pageId) {
        return String.format(LATEST_VERSION_KEY_TEMPLATE, pageId);
    }

    private String pageVersionsKey(Long pageId) {
        return String.format(PAGE_VERSIONS_KEY_TEMPLATE, pageId);
    }

    private String versionByIdKey(Long versionId) {
        return String.format(VERSION_BY_ID_KEY_TEMPLATE, versionId);
    }
}
