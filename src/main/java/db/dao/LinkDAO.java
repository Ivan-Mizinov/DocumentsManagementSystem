package db.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import db.dto.LinkDTO;
import db.entities.Link;
import db.entities.Page;
import db.util.RedisCacheUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.stream.Collectors;

public class LinkDAO extends BaseDAO<Link, LinkDTO> {
    private static final String LINKS_BY_PAGE_KEY_TEMPLATE = "page:%d:links";
    private static final TypeReference<List<LinkDTO>> LINK_LIST_TYPE = new TypeReference<>() {};

    public LinkDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    protected LinkDTO entityToDTO(Link entity) {
        if (entity == null) return null;
        Long pageId = entity.getPage() != null ? entity.getPage().getId() : null;
        return new LinkDTO(
                entity.getId(),
                pageId,
                entity.getUrl(),
                entity.getTitle(),
                entity.getDescription()
        );
    }

    @Override
    protected Link dtoToEntity(LinkDTO dto) {
        if (dto == null) return null;
        Link link = new Link();
        link.setId(dto.getId());
        link.setUrl(dto.getUrl());
        link.setTitle(dto.getTitle());
        link.setDescription(dto.getDescription());

        if (dto.getPageId() != null) {
            try (Session session = getSession()) {
                Page page = session.find(Page.class, dto.getPageId());
                link.setPage(page);
            }
        }
        return link;
    }

    @Override
    protected Class<LinkDTO> getDTOClass() {
        return LinkDTO.class;
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
        List<LinkDTO> cachedDTOs = RedisCacheUtil.getValue(key, LINK_LIST_TYPE);
        if (cachedDTOs != null) {
            return cachedDTOs.stream().map(this::dtoToEntity).collect(Collectors.toList());
        }
        try (Session session = getSession()) {
            List<Link> links = session.createQuery(
                            "FROM Link l WHERE l.page.id = :pageId",
                            Link.class)
                    .setParameter("pageId", pageId)
                    .list();
            List<LinkDTO> DTOs = links.stream().map(this::entityToDTO).collect(Collectors.toList());
            RedisCacheUtil.cacheValue(key, DTOs);
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
