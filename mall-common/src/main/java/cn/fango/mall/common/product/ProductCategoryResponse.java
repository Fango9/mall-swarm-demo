package cn.fango.mall.common.product;

/**
 * 商品分类查询响应。
 *
 * @param id 商品分类主键
 * @param parentId 父分类主键
 * @param name 分类名称
 * @param level 分类层级
 * @param sort 排序值
 */
public record ProductCategoryResponse(
        Long id,
        Long parentId,
        String name,
        Byte level,
        Integer sort
) {
}