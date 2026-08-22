package cn.fango.mall.admin.dto;

/**
 * 更新品牌请求。
 *
 * @param name 品牌名称
 * @param firstLetter 品牌首字母
 * @param sort 排序值
 * @param factoryStatus 是否为品牌制造商
 * @param showStatus 是否显示
 * @param logo 品牌 Logo
 * @param bigPic 品牌专区大图
 * @param brandStory 品牌故事
 */
public record BrandUpdateRequest(
        String name,
        String firstLetter,
        Integer sort,
        Byte factoryStatus,
        Byte showStatus,
        String logo,
        String bigPic,
        String brandStory
) {
}