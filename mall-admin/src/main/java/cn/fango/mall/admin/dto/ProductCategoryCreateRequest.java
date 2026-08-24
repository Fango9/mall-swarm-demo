package cn.fango.mall.admin.dto;

/**
 * 创建商品分类请求。
 *
 * @param parentId 父分类主键，一级分类传 0
 * @param name 分类名称
 * @param level 分类层级：0->一级；1->二级
 * @param sort 排序值
 * @param showStatus 是否显示：0->不显示；1->显示
 */
public record ProductCategoryCreateRequest(
        Long parentId,
        String name,
        Byte level,
        Integer sort,
        Byte showStatus
) {
}