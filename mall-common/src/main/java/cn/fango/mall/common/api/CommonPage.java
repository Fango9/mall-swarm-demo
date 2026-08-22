package cn.fango.mall.common.api;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * 统一分页响应对象。
 *
 * @param <T> 当前页列表元素类型
 */
public class CommonPage<T> {

    private Integer pageNum;
    private Integer pageSize;
    private Integer totalPage;
    private Long total;
    private List<T> list;

    /**
     * 将 PageHelper 分页列表转换为统一分页响应。
     *
     * @param list PageHelper 返回的分页列表
     * @param <T> 列表元素类型
     * @return 统一分页响应
     */
    public static <T> CommonPage<T> restPage(List<T> list) {
        PageInfo<T> pageInfo = new PageInfo<>(list);
        CommonPage<T> result = new CommonPage<>();

        result.setTotalPage(pageInfo.getPages());
        result.setPageNum(pageInfo.getPageNum());
        result.setPageSize(pageInfo.getPageSize());
        result.setTotal(pageInfo.getTotal());
        result.setList(pageInfo.getList());

        return result;
    }

    /**
     * 将 PageHelper 的 Page 对象转换为统一分页响应。
     *
     * @param page PageHelper 分页对象
     * @param <T> 列表元素类型
     * @return 统一分页响应
     */
    public static <T> CommonPage<T> restPage(Page<T> page) {
        CommonPage<T> result = new CommonPage<>();

        result.setTotalPage(page.getPages());
        result.setPageNum(page.getPageNum());
        result.setPageSize(page.getPageSize());
        result.setTotal(page.getTotal());
        result.setList(page);

        return result;
    }

    /**
     * 获取当前页码。
     *
     * @return 当前页码，从 1 开始
     */
    public Integer getPageNum() {
        return pageNum;
    }

    /**
     * 设置当前页码。
     *
     * @param pageNum 当前页码
     */
    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    /**
     * 获取每页记录数。
     *
     * @return 每页记录数
     */
    public Integer getPageSize() {
        return pageSize;
    }

    /**
     * 设置每页记录数。
     *
     * @param pageSize 每页记录数
     */
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * 获取总页数。
     *
     * @return 总页数
     */
    public Integer getTotalPage() {
        return totalPage;
    }

    /**
     * 设置总页数。
     *
     * @param totalPage 总页数
     */
    public void setTotalPage(Integer totalPage) {
        this.totalPage = totalPage;
    }

    /**
     * 获取总记录数。
     *
     * @return 总记录数
     */
    public Long getTotal() {
        return total;
    }

    /**
     * 设置总记录数。
     *
     * @param total 总记录数
     */
    public void setTotal(Long total) {
        this.total = total;
    }

    /**
     * 获取当前页数据。
     *
     * @return 当前页数据
     */
    public List<T> getList() {
        return list;
    }

    /**
     * 设置当前页数据。
     *
     * @param list 当前页数据
     */
    public void setList(List<T> list) {
        this.list = list;
    }
}