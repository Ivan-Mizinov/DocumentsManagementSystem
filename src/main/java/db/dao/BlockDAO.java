package db.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import db.dto.BlockDTO;
import db.entities.Block;
import db.entities.Page;
import db.util.RedisCacheUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.stream.Collectors;

public class BlockDAO extends BaseDAO<Block, BlockDTO> {
    private static final String BLOCKS_BY_PAGE_KEY_TEMPLATE = "page:%d:blocks";
    private static final TypeReference<List<BlockDTO>> BLOCK_LIST_TYPE = new TypeReference<>() {};

    public BlockDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    protected BlockDTO entityToDTO(Block entity) {
        if (entity == null) return null;
        Long pageId = entity.getPage() != null ? entity.getPage().getId() : null;
        return new BlockDTO(
                entity.getId(),
                pageId,
                entity.getType(),
                entity.getContent(),
                entity.getPosition(),
                entity.isVisible()
        );
    }

    @Override
    protected Block dtoToEntity(BlockDTO dto) {
        if (dto == null) return null;
        Block block = new Block();
        block.setId(dto.getId());
        block.setType(dto.getType());
        block.setContent(dto.getContent());
        block.setPosition(dto.getPosition());
        block.setVisible(dto.isVisible());

        if (dto.getPageId() != null) {
            try (Session session = getSession()) {
                Page page = session.find(Page.class, dto.getPageId());
                block.setPage(page);
            }
        }
        return block;
    }

    @Override
    protected Class<BlockDTO> getDTOClass() {
        return BlockDTO.class;
    }

    @Override
    public Block save(Block block) {
        Block saved = super.save(block);
        evictBlocksByPage(saved);
        return saved;
    }

    @Override
    public Block update(Block block) {
        Block updated = super.update(block);
        evictBlocksByPage(updated);
        return updated;
    }

    @Override
    public void delete(Block block) {
        Long pageId = block.getPage() != null ? block.getPage().getId() : null;
        super.delete(block);
        if (pageId != null) {
            RedisCacheUtil.evict(blocksKey(pageId));
        }
    }

    public List<Block> getAllBlocks() {
        try (Session session = getSession()) {
            return session.createQuery("FROM Block", Block.class).list();
        }
    }

    public List<Block> getBlocksByPageId(Long pageId) {
        String key = blocksKey(pageId);
        List<BlockDTO> cachedDTOs = RedisCacheUtil.getValue(key, BLOCK_LIST_TYPE);
        if (cachedDTOs != null) {
            return cachedDTOs.stream().map(this::dtoToEntity).collect(Collectors.toList());
        }
        try (Session session = getSession()) {
            List<Block> blocks = session.createQuery(
                            "FROM Block b WHERE b.page.id = :pageId", Block.class)
                    .setParameter("pageId", pageId)
                    .list();
            List<BlockDTO> DTOs = blocks.stream().map(this::entityToDTO).collect(Collectors.toList());
            RedisCacheUtil.cacheValue(key, DTOs);
            return blocks;
        }
    }

    private void evictBlocksByPage(Block block) {
        if (block != null && block.getPage() != null && block.getPage().getId() != null) {
            RedisCacheUtil.evict(blocksKey(block.getPage().getId()));
        }
    }

    private String blocksKey(Long pageId) {
        return String.format(BLOCKS_BY_PAGE_KEY_TEMPLATE, pageId);
    }
}
