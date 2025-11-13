package db.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import db.entities.Block;
import db.util.RedisCacheUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class BlockDAO extends BaseDAO<Block> {
    private static final String BLOCKS_BY_PAGE_KEY_TEMPLATE = "page:%d:blocks";
    private static final TypeReference<List<Block>> BLOCK_LIST_TYPE = new TypeReference<>() {};

    public BlockDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
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
        List<Block> cached = RedisCacheUtil.getValue(key, BLOCK_LIST_TYPE);
        if (cached != null) {
            return cached;
        }
        try (Session session = getSession()) {
            List<Block> blocks = session.createQuery(
                            "FROM Block b WHERE b.page.id = :pageId", Block.class)
                    .setParameter("pageId", pageId)
                    .list();
            RedisCacheUtil.cacheValue(key, blocks);
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
