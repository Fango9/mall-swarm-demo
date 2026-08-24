package cn.fango.mall.admin.dto;

/**
 * 更新商品分类请求。
 *
 * @param name 分类名称
 * @param sort 排序值
 * @param showStatus 是否显示：0->不显示；1->显示
 */
public record ProductCategoryUpdateRequest(
        String name,
        Integer sort,
        Byte showStatus
) {
}