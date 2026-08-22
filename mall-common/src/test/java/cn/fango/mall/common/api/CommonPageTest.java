package cn.fango.mall.common.api;

import com.github.pagehelper.Page;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link CommonPage} 的单元测试。
 */
class CommonPageTest {

    /**
     * 验证普通列表可以转换为单页分页响应。
     */
    @Test
    void shouldCreatePageFromList() {
        List<String> sourceList = List.of("商品A", "商品B");

        CommonPage<String> result = CommonPage.restPage(sourceList);

        assertEquals(1, result.getPageNum());
        assertEquals(2, result.getPageSize());
        assertEquals(1, result.getTotalPage());
        assertEquals(2L, result.getTotal());
        assertEquals(sourceList, result.getList());
    }

    /**
     * 验证 PageHelper 的分页元数据会被完整保留。
     */
    @Test
    void shouldCreatePageFromPageHelperPage() {
        Page<String> sourcePage = new Page<>(2, 2);
        sourcePage.setTotal(5);
        sourcePage.add("商品C");

        CommonPage<String> result = CommonPage.restPage(sourcePage);

        assertEquals(2, result.getPageNum());
        assertEquals(2, result.getPageSize());
        assertEquals(3, result.getTotalPage());
        assertEquals(5L, result.getTotal());
        assertEquals(List.of("商品C"), result.getList());
    }
}