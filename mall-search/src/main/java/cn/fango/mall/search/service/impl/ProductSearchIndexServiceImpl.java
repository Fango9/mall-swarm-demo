package cn.fango.mall.search.service.impl;

import cn.fango.mall.common.api.CommonResult;
import cn.fango.mall.common.api.ResultCode;
import cn.fango.mall.common.search.ProductSearchSourceResponse;
import cn.fango.mall.search.client.SearchProductSourceClient;
import cn.fango.mall.search.document.ProductSearchDocument;
import cn.fango.mall.search.repository.ProductSearchRepository;
import cn.fango.mall.search.service.ProductSearchIndexService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 商品搜索索引同步服务实现。
 */
@Service
public class ProductSearchIndexServiceImpl implements ProductSearchIndexService {

    /**
     * Admin 商品索引源数据 Feign 客户端。
     */
    private final SearchProductSourceClient searchProductSourceClient;

    /**
     * 商品搜索索引数据访问接口。
     */
    private final ProductSearchRepository productSearchRepository;

    /**
     * 协调全量重建与单商品增量同步的本地读写锁。
     */
    private final ReentrantReadWriteLock indexSynchronizationLock =
            new ReentrantReadWriteLock();

    /**
     * 创建商品搜索索引同步服务。
     *
     * @param searchProductSourceClient Admin 商品索引源数据 Feign 客户端
     * @param productSearchRepository 商品搜索索引数据访问接口
     */
    public ProductSearchIndexServiceImpl(
            SearchProductSourceClient searchProductSourceClient,
            ProductSearchRepository productSearchRepository
    ) {
        this.searchProductSourceClient = searchProductSourceClient;
        this.productSearchRepository = productSearchRepository;
    }

    /**
     * 将指定商品的当前发布状态同步到 Elasticsearch。
     *
     * @param productId 商品主键
     */
    @Override
    public void synchronizeProduct(Long productId) {
        indexSynchronizationLock.readLock().lock();

        try {
            if (productId == null || productId <= 0) {
                throw new IllegalArgumentException("商品主键必须大于 0");
            }

            CommonResult<ProductSearchSourceResponse> sourceResult =
                    searchProductSourceClient.getPublishedProductForIndex(productId);

            if (sourceResult == null
                    || sourceResult.getCode() != ResultCode.SUCCESS.getCode()) {
                throw new IllegalStateException("查询商品搜索索引源数据失败");
            }

            ProductSearchSourceResponse source = sourceResult.getData();
            if (source == null) {
                productSearchRepository.deleteById(productId);
                return;
            }

            if (source.id() == null || !productId.equals(source.id())) {
                throw new IllegalStateException("商品搜索索引源数据的商品主键不一致");
            }

            productSearchRepository.save(toDocument(source));
        } finally {
            indexSynchronizationLock.readLock().unlock();
        }
    }

    /**
     * 使用 Admin 当前全部可索引商品重建 Elasticsearch 商品索引。
     */
    @Override
    public void rebuildAllProducts() {
        indexSynchronizationLock.writeLock().lock();

        try {
            CommonResult<List<ProductSearchSourceResponse>> sourceResult =
                    searchProductSourceClient.listPublishedProductsForIndex();

            if (sourceResult == null
                    || sourceResult.getCode() != ResultCode.SUCCESS.getCode()
                    || sourceResult.getData() == null) {
                throw new IllegalStateException("查询全部商品搜索索引源数据失败");
            }

            List<ProductSearchDocument> documents = new ArrayList<>();
            Set<Long> productIds = new HashSet<>();

            for (ProductSearchSourceResponse source : sourceResult.getData()) {
                if (source == null
                        || source.id() == null
                        || source.id() <= 0
                        || !productIds.add(source.id())) {
                    throw new IllegalStateException("全量索引源数据包含非法或重复商品主键");
                }

                documents.add(toDocument(source));
            }

            productSearchRepository.deleteAll();
            productSearchRepository.saveAll(documents);
        } finally {
            indexSynchronizationLock.writeLock().unlock();
        }
    }

    /**
     * 将 Admin 索引源数据转换为 Elasticsearch 索引文档。
     *
     * @param source Admin 提供的可索引商品数据
     * @return Elasticsearch 商品搜索索引文档
     */
    private ProductSearchDocument toDocument(ProductSearchSourceResponse source) {
        return new ProductSearchDocument(
                source.id(),
                source.productCategoryId(),
                source.name(),
                source.productSn(),
                source.price(),
                source.stock(),
                source.pic(),
                source.detailDesc()
        );
    }
}