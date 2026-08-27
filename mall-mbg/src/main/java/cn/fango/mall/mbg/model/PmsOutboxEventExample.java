package cn.fango.mall.mbg.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PmsOutboxEventExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public PmsOutboxEventExample() {
        oredCriteria = new ArrayList<>();
    }

    public void setOrderByClause(String orderByClause) {
        this.orderByClause = orderByClause;
    }

    public String getOrderByClause() {
        return orderByClause;
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public List<Criteria> getOredCriteria() {
        return oredCriteria;
    }

    public void or(Criteria criteria) {
        oredCriteria.add(criteria);
    }

    public Criteria or() {
        Criteria criteria = createCriteriaInternal();
        oredCriteria.add(criteria);
        return criteria;
    }

    public Criteria createCriteria() {
        Criteria criteria = createCriteriaInternal();
        if (oredCriteria.size() == 0) {
            oredCriteria.add(criteria);
        }
        return criteria;
    }

    protected Criteria createCriteriaInternal() {
        Criteria criteria = new Criteria();
        return criteria;
    }

    public void clear() {
        oredCriteria.clear();
        orderByClause = null;
        distinct = false;
    }

    protected abstract static class GeneratedCriteria {
        protected List<Criterion> criteria;

        protected GeneratedCriteria() {
            super();
            criteria = new ArrayList<>();
        }

        public boolean isValid() {
            return criteria.size() > 0;
        }

        public List<Criterion> getAllCriteria() {
            return criteria;
        }

        public List<Criterion> getCriteria() {
            return criteria;
        }

        protected void addCriterion(String condition) {
            if (condition == null) {
                throw new RuntimeException("Value for condition cannot be null");
            }
            criteria.add(new Criterion(condition));
        }

        protected void addCriterion(String condition, Object value, String property) {
            if (value == null) {
                throw new RuntimeException("Value for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value));
        }

        protected void addCriterion(String condition, Object value1, Object value2, String property) {
            if (value1 == null || value2 == null) {
                throw new RuntimeException("Between values for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value1, value2));
        }

        public Criteria andIdIsNull() {
            addCriterion("id is null");
            return (Criteria) this;
        }

        public Criteria andIdIsNotNull() {
            addCriterion("id is not null");
            return (Criteria) this;
        }

        public Criteria andIdEqualTo(Long value) {
            addCriterion("id =", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotEqualTo(Long value) {
            addCriterion("id <>", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdGreaterThan(Long value) {
            addCriterion("id >", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdGreaterThanOrEqualTo(Long value) {
            addCriterion("id >=", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdLessThan(Long value) {
            addCriterion("id <", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdLessThanOrEqualTo(Long value) {
            addCriterion("id <=", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdIn(List<Long> values) {
            addCriterion("id in", values, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotIn(List<Long> values) {
            addCriterion("id not in", values, "id");
            return (Criteria) this;
        }

        public Criteria andIdBetween(Long value1, Long value2) {
            addCriterion("id between", value1, value2, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotBetween(Long value1, Long value2) {
            addCriterion("id not between", value1, value2, "id");
            return (Criteria) this;
        }

        public Criteria andEventIdIsNull() {
            addCriterion("event_id is null");
            return (Criteria) this;
        }

        public Criteria andEventIdIsNotNull() {
            addCriterion("event_id is not null");
            return (Criteria) this;
        }

        public Criteria andEventIdEqualTo(String value) {
            addCriterion("event_id =", value, "eventId");
            return (Criteria) this;
        }

        public Criteria andEventIdNotEqualTo(String value) {
            addCriterion("event_id <>", value, "eventId");
            return (Criteria) this;
        }

        public Criteria andEventIdGreaterThan(String value) {
            addCriterion("event_id >", value, "eventId");
            return (Criteria) this;
        }

        public Criteria andEventIdGreaterThanOrEqualTo(String value) {
            addCriterion("event_id >=", value, "eventId");
            return (Criteria) this;
        }

        public Criteria andEventIdLessThan(String value) {
            addCriterion("event_id <", value, "eventId");
            return (Criteria) this;
        }

        public Criteria andEventIdLessThanOrEqualTo(String value) {
            addCriterion("event_id <=", value, "eventId");
            return (Criteria) this;
        }

        public Criteria andEventIdLike(String value) {
            addCriterion("event_id like", value, "eventId");
            return (Criteria) this;
        }

        public Criteria andEventIdNotLike(String value) {
            addCriterion("event_id not like", value, "eventId");
            return (Criteria) this;
        }

        public Criteria andEventIdIn(List<String> values) {
            addCriterion("event_id in", values, "eventId");
            return (Criteria) this;
        }

        public Criteria andEventIdNotIn(List<String> values) {
            addCriterion("event_id not in", values, "eventId");
            return (Criteria) this;
        }

        public Criteria andEventIdBetween(String value1, String value2) {
            addCriterion("event_id between", value1, value2, "eventId");
            return (Criteria) this;
        }

        public Criteria andEventIdNotBetween(String value1, String value2) {
            addCriterion("event_id not between", value1, value2, "eventId");
            return (Criteria) this;
        }

        public Criteria andAggregateTypeIsNull() {
            addCriterion("aggregate_type is null");
            return (Criteria) this;
        }

        public Criteria andAggregateTypeIsNotNull() {
            addCriterion("aggregate_type is not null");
            return (Criteria) this;
        }

        public Criteria andAggregateTypeEqualTo(String value) {
            addCriterion("aggregate_type =", value, "aggregateType");
            return (Criteria) this;
        }

        public Criteria andAggregateTypeNotEqualTo(String value) {
            addCriterion("aggregate_type <>", value, "aggregateType");
            return (Criteria) this;
        }

        public Criteria andAggregateTypeGreaterThan(String value) {
            addCriterion("aggregate_type >", value, "aggregateType");
            return (Criteria) this;
        }

        public Criteria andAggregateTypeGreaterThanOrEqualTo(String value) {
            addCriterion("aggregate_type >=", value, "aggregateType");
            return (Criteria) this;
        }

        public Criteria andAggregateTypeLessThan(String value) {
            addCriterion("aggregate_type <", value, "aggregateType");
            return (Criteria) this;
        }

        public Criteria andAggregateTypeLessThanOrEqualTo(String value) {
            addCriterion("aggregate_type <=", value, "aggregateType");
            return (Criteria) this;
        }

        public Criteria andAggregateTypeLike(String value) {
            addCriterion("aggregate_type like", value, "aggregateType");
            return (Criteria) this;
        }

        public Criteria andAggregateTypeNotLike(String value) {
            addCriterion("aggregate_type not like", value, "aggregateType");
            return (Criteria) this;
        }

        public Criteria andAggregateTypeIn(List<String> values) {
            addCriterion("aggregate_type in", values, "aggregateType");
            return (Criteria) this;
        }

        public Criteria andAggregateTypeNotIn(List<String> values) {
            addCriterion("aggregate_type not in", values, "aggregateType");
            return (Criteria) this;
        }

        public Criteria andAggregateTypeBetween(String value1, String value2) {
            addCriterion("aggregate_type between", value1, value2, "aggregateType");
            return (Criteria) this;
        }

        public Criteria andAggregateTypeNotBetween(String value1, String value2) {
            addCriterion("aggregate_type not between", value1, value2, "aggregateType");
            return (Criteria) this;
        }

        public Criteria andAggregateIdIsNull() {
            addCriterion("aggregate_id is null");
            return (Criteria) this;
        }

        public Criteria andAggregateIdIsNotNull() {
            addCriterion("aggregate_id is not null");
            return (Criteria) this;
        }

        public Criteria andAggregateIdEqualTo(Long value) {
            addCriterion("aggregate_id =", value, "aggregateId");
            return (Criteria) this;
        }

        public Criteria andAggregateIdNotEqualTo(Long value) {
            addCriterion("aggregate_id <>", value, "aggregateId");
            return (Criteria) this;
        }

        public Criteria andAggregateIdGreaterThan(Long value) {
            addCriterion("aggregate_id >", value, "aggregateId");
            return (Criteria) this;
        }

        public Criteria andAggregateIdGreaterThanOrEqualTo(Long value) {
            addCriterion("aggregate_id >=", value, "aggregateId");
            return (Criteria) this;
        }

        public Criteria andAggregateIdLessThan(Long value) {
            addCriterion("aggregate_id <", value, "aggregateId");
            return (Criteria) this;
        }

        public Criteria andAggregateIdLessThanOrEqualTo(Long value) {
            addCriterion("aggregate_id <=", value, "aggregateId");
            return (Criteria) this;
        }

        public Criteria andAggregateIdIn(List<Long> values) {
            addCriterion("aggregate_id in", values, "aggregateId");
            return (Criteria) this;
        }

        public Criteria andAggregateIdNotIn(List<Long> values) {
            addCriterion("aggregate_id not in", values, "aggregateId");
            return (Criteria) this;
        }

        public Criteria andAggregateIdBetween(Long value1, Long value2) {
            addCriterion("aggregate_id between", value1, value2, "aggregateId");
            return (Criteria) this;
        }

        public Criteria andAggregateIdNotBetween(Long value1, Long value2) {
            addCriterion("aggregate_id not between", value1, value2, "aggregateId");
            return (Criteria) this;
        }

        public Criteria andEventTypeIsNull() {
            addCriterion("event_type is null");
            return (Criteria) this;
        }

        public Criteria andEventTypeIsNotNull() {
            addCriterion("event_type is not null");
            return (Criteria) this;
        }

        public Criteria andEventTypeEqualTo(String value) {
            addCriterion("event_type =", value, "eventType");
            return (Criteria) this;
        }

        public Criteria andEventTypeNotEqualTo(String value) {
            addCriterion("event_type <>", value, "eventType");
            return (Criteria) this;
        }

        public Criteria andEventTypeGreaterThan(String value) {
            addCriterion("event_type >", value, "eventType");
            return (Criteria) this;
        }

        public Criteria andEventTypeGreaterThanOrEqualTo(String value) {
            addCriterion("event_type >=", value, "eventType");
            return (Criteria) this;
        }

        public Criteria andEventTypeLessThan(String value) {
            addCriterion("event_type <", value, "eventType");
            return (Criteria) this;
        }

        public Criteria andEventTypeLessThanOrEqualTo(String value) {
            addCriterion("event_type <=", value, "eventType");
            return (Criteria) this;
        }

        public Criteria andEventTypeLike(String value) {
            addCriterion("event_type like", value, "eventType");
            return (Criteria) this;
        }

        public Criteria andEventTypeNotLike(String value) {
            addCriterion("event_type not like", value, "eventType");
            return (Criteria) this;
        }

        public Criteria andEventTypeIn(List<String> values) {
            addCriterion("event_type in", values, "eventType");
            return (Criteria) this;
        }

        public Criteria andEventTypeNotIn(List<String> values) {
            addCriterion("event_type not in", values, "eventType");
            return (Criteria) this;
        }

        public Criteria andEventTypeBetween(String value1, String value2) {
            addCriterion("event_type between", value1, value2, "eventType");
            return (Criteria) this;
        }

        public Criteria andEventTypeNotBetween(String value1, String value2) {
            addCriterion("event_type not between", value1, value2, "eventType");
            return (Criteria) this;
        }

        public Criteria andStatusIsNull() {
            addCriterion("status is null");
            return (Criteria) this;
        }

        public Criteria andStatusIsNotNull() {
            addCriterion("status is not null");
            return (Criteria) this;
        }

        public Criteria andStatusEqualTo(String value) {
            addCriterion("status =", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotEqualTo(String value) {
            addCriterion("status <>", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusGreaterThan(String value) {
            addCriterion("status >", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusGreaterThanOrEqualTo(String value) {
            addCriterion("status >=", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusLessThan(String value) {
            addCriterion("status <", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusLessThanOrEqualTo(String value) {
            addCriterion("status <=", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusLike(String value) {
            addCriterion("status like", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotLike(String value) {
            addCriterion("status not like", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusIn(List<String> values) {
            addCriterion("status in", values, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotIn(List<String> values) {
            addCriterion("status not in", values, "status");
            return (Criteria) this;
        }

        public Criteria andStatusBetween(String value1, String value2) {
            addCriterion("status between", value1, value2, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotBetween(String value1, String value2) {
            addCriterion("status not between", value1, value2, "status");
            return (Criteria) this;
        }

        public Criteria andRetryCountIsNull() {
            addCriterion("retry_count is null");
            return (Criteria) this;
        }

        public Criteria andRetryCountIsNotNull() {
            addCriterion("retry_count is not null");
            return (Criteria) this;
        }

        public Criteria andRetryCountEqualTo(Integer value) {
            addCriterion("retry_count =", value, "retryCount");
            return (Criteria) this;
        }

        public Criteria andRetryCountNotEqualTo(Integer value) {
            addCriterion("retry_count <>", value, "retryCount");
            return (Criteria) this;
        }

        public Criteria andRetryCountGreaterThan(Integer value) {
            addCriterion("retry_count >", value, "retryCount");
            return (Criteria) this;
        }

        public Criteria andRetryCountGreaterThanOrEqualTo(Integer value) {
            addCriterion("retry_count >=", value, "retryCount");
            return (Criteria) this;
        }

        public Criteria andRetryCountLessThan(Integer value) {
            addCriterion("retry_count <", value, "retryCount");
            return (Criteria) this;
        }

        public Criteria andRetryCountLessThanOrEqualTo(Integer value) {
            addCriterion("retry_count <=", value, "retryCount");
            return (Criteria) this;
        }

        public Criteria andRetryCountIn(List<Integer> values) {
            addCriterion("retry_count in", values, "retryCount");
            return (Criteria) this;
        }

        public Criteria andRetryCountNotIn(List<Integer> values) {
            addCriterion("retry_count not in", values, "retryCount");
            return (Criteria) this;
        }

        public Criteria andRetryCountBetween(Integer value1, Integer value2) {
            addCriterion("retry_count between", value1, value2, "retryCount");
            return (Criteria) this;
        }

        public Criteria andRetryCountNotBetween(Integer value1, Integer value2) {
            addCriterion("retry_count not between", value1, value2, "retryCount");
            return (Criteria) this;
        }

        public Criteria andNextRetryAtIsNull() {
            addCriterion("next_retry_at is null");
            return (Criteria) this;
        }

        public Criteria andNextRetryAtIsNotNull() {
            addCriterion("next_retry_at is not null");
            return (Criteria) this;
        }

        public Criteria andNextRetryAtEqualTo(Date value) {
            addCriterion("next_retry_at =", value, "nextRetryAt");
            return (Criteria) this;
        }

        public Criteria andNextRetryAtNotEqualTo(Date value) {
            addCriterion("next_retry_at <>", value, "nextRetryAt");
            return (Criteria) this;
        }

        public Criteria andNextRetryAtGreaterThan(Date value) {
            addCriterion("next_retry_at >", value, "nextRetryAt");
            return (Criteria) this;
        }

        public Criteria andNextRetryAtGreaterThanOrEqualTo(Date value) {
            addCriterion("next_retry_at >=", value, "nextRetryAt");
            return (Criteria) this;
        }

        public Criteria andNextRetryAtLessThan(Date value) {
            addCriterion("next_retry_at <", value, "nextRetryAt");
            return (Criteria) this;
        }

        public Criteria andNextRetryAtLessThanOrEqualTo(Date value) {
            addCriterion("next_retry_at <=", value, "nextRetryAt");
            return (Criteria) this;
        }

        public Criteria andNextRetryAtIn(List<Date> values) {
            addCriterion("next_retry_at in", values, "nextRetryAt");
            return (Criteria) this;
        }

        public Criteria andNextRetryAtNotIn(List<Date> values) {
            addCriterion("next_retry_at not in", values, "nextRetryAt");
            return (Criteria) this;
        }

        public Criteria andNextRetryAtBetween(Date value1, Date value2) {
            addCriterion("next_retry_at between", value1, value2, "nextRetryAt");
            return (Criteria) this;
        }

        public Criteria andNextRetryAtNotBetween(Date value1, Date value2) {
            addCriterion("next_retry_at not between", value1, value2, "nextRetryAt");
            return (Criteria) this;
        }

        public Criteria andPublishedAtIsNull() {
            addCriterion("published_at is null");
            return (Criteria) this;
        }

        public Criteria andPublishedAtIsNotNull() {
            addCriterion("published_at is not null");
            return (Criteria) this;
        }

        public Criteria andPublishedAtEqualTo(Date value) {
            addCriterion("published_at =", value, "publishedAt");
            return (Criteria) this;
        }

        public Criteria andPublishedAtNotEqualTo(Date value) {
            addCriterion("published_at <>", value, "publishedAt");
            return (Criteria) this;
        }

        public Criteria andPublishedAtGreaterThan(Date value) {
            addCriterion("published_at >", value, "publishedAt");
            return (Criteria) this;
        }

        public Criteria andPublishedAtGreaterThanOrEqualTo(Date value) {
            addCriterion("published_at >=", value, "publishedAt");
            return (Criteria) this;
        }

        public Criteria andPublishedAtLessThan(Date value) {
            addCriterion("published_at <", value, "publishedAt");
            return (Criteria) this;
        }

        public Criteria andPublishedAtLessThanOrEqualTo(Date value) {
            addCriterion("published_at <=", value, "publishedAt");
            return (Criteria) this;
        }

        public Criteria andPublishedAtIn(List<Date> values) {
            addCriterion("published_at in", values, "publishedAt");
            return (Criteria) this;
        }

        public Criteria andPublishedAtNotIn(List<Date> values) {
            addCriterion("published_at not in", values, "publishedAt");
            return (Criteria) this;
        }

        public Criteria andPublishedAtBetween(Date value1, Date value2) {
            addCriterion("published_at between", value1, value2, "publishedAt");
            return (Criteria) this;
        }

        public Criteria andPublishedAtNotBetween(Date value1, Date value2) {
            addCriterion("published_at not between", value1, value2, "publishedAt");
            return (Criteria) this;
        }

        public Criteria andLastErrorIsNull() {
            addCriterion("last_error is null");
            return (Criteria) this;
        }

        public Criteria andLastErrorIsNotNull() {
            addCriterion("last_error is not null");
            return (Criteria) this;
        }

        public Criteria andLastErrorEqualTo(String value) {
            addCriterion("last_error =", value, "lastError");
            return (Criteria) this;
        }

        public Criteria andLastErrorNotEqualTo(String value) {
            addCriterion("last_error <>", value, "lastError");
            return (Criteria) this;
        }

        public Criteria andLastErrorGreaterThan(String value) {
            addCriterion("last_error >", value, "lastError");
            return (Criteria) this;
        }

        public Criteria andLastErrorGreaterThanOrEqualTo(String value) {
            addCriterion("last_error >=", value, "lastError");
            return (Criteria) this;
        }

        public Criteria andLastErrorLessThan(String value) {
            addCriterion("last_error <", value, "lastError");
            return (Criteria) this;
        }

        public Criteria andLastErrorLessThanOrEqualTo(String value) {
            addCriterion("last_error <=", value, "lastError");
            return (Criteria) this;
        }

        public Criteria andLastErrorLike(String value) {
            addCriterion("last_error like", value, "lastError");
            return (Criteria) this;
        }

        public Criteria andLastErrorNotLike(String value) {
            addCriterion("last_error not like", value, "lastError");
            return (Criteria) this;
        }

        public Criteria andLastErrorIn(List<String> values) {
            addCriterion("last_error in", values, "lastError");
            return (Criteria) this;
        }

        public Criteria andLastErrorNotIn(List<String> values) {
            addCriterion("last_error not in", values, "lastError");
            return (Criteria) this;
        }

        public Criteria andLastErrorBetween(String value1, String value2) {
            addCriterion("last_error between", value1, value2, "lastError");
            return (Criteria) this;
        }

        public Criteria andLastErrorNotBetween(String value1, String value2) {
            addCriterion("last_error not between", value1, value2, "lastError");
            return (Criteria) this;
        }

        public Criteria andCreateTimeIsNull() {
            addCriterion("create_time is null");
            return (Criteria) this;
        }

        public Criteria andCreateTimeIsNotNull() {
            addCriterion("create_time is not null");
            return (Criteria) this;
        }

        public Criteria andCreateTimeEqualTo(Date value) {
            addCriterion("create_time =", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeNotEqualTo(Date value) {
            addCriterion("create_time <>", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeGreaterThan(Date value) {
            addCriterion("create_time >", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeGreaterThanOrEqualTo(Date value) {
            addCriterion("create_time >=", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeLessThan(Date value) {
            addCriterion("create_time <", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeLessThanOrEqualTo(Date value) {
            addCriterion("create_time <=", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeIn(List<Date> values) {
            addCriterion("create_time in", values, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeNotIn(List<Date> values) {
            addCriterion("create_time not in", values, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeBetween(Date value1, Date value2) {
            addCriterion("create_time between", value1, value2, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeNotBetween(Date value1, Date value2) {
            addCriterion("create_time not between", value1, value2, "createTime");
            return (Criteria) this;
        }

        public Criteria andModifyTimeIsNull() {
            addCriterion("modify_time is null");
            return (Criteria) this;
        }

        public Criteria andModifyTimeIsNotNull() {
            addCriterion("modify_time is not null");
            return (Criteria) this;
        }

        public Criteria andModifyTimeEqualTo(Date value) {
            addCriterion("modify_time =", value, "modifyTime");
            return (Criteria) this;
        }

        public Criteria andModifyTimeNotEqualTo(Date value) {
            addCriterion("modify_time <>", value, "modifyTime");
            return (Criteria) this;
        }

        public Criteria andModifyTimeGreaterThan(Date value) {
            addCriterion("modify_time >", value, "modifyTime");
            return (Criteria) this;
        }

        public Criteria andModifyTimeGreaterThanOrEqualTo(Date value) {
            addCriterion("modify_time >=", value, "modifyTime");
            return (Criteria) this;
        }

        public Criteria andModifyTimeLessThan(Date value) {
            addCriterion("modify_time <", value, "modifyTime");
            return (Criteria) this;
        }

        public Criteria andModifyTimeLessThanOrEqualTo(Date value) {
            addCriterion("modify_time <=", value, "modifyTime");
            return (Criteria) this;
        }

        public Criteria andModifyTimeIn(List<Date> values) {
            addCriterion("modify_time in", values, "modifyTime");
            return (Criteria) this;
        }

        public Criteria andModifyTimeNotIn(List<Date> values) {
            addCriterion("modify_time not in", values, "modifyTime");
            return (Criteria) this;
        }

        public Criteria andModifyTimeBetween(Date value1, Date value2) {
            addCriterion("modify_time between", value1, value2, "modifyTime");
            return (Criteria) this;
        }

        public Criteria andModifyTimeNotBetween(Date value1, Date value2) {
            addCriterion("modify_time not between", value1, value2, "modifyTime");
            return (Criteria) this;
        }
    }

    public static class Criteria extends GeneratedCriteria {
        protected Criteria() {
            super();
        }
    }

    public static class Criterion {
        private String condition;

        private Object value;

        private Object secondValue;

        private boolean noValue;

        private boolean singleValue;

        private boolean betweenValue;

        private boolean listValue;

        private String typeHandler;

        public String getCondition() {
            return condition;
        }

        public Object getValue() {
            return value;
        }

        public Object getSecondValue() {
            return secondValue;
        }

        public boolean isNoValue() {
            return noValue;
        }

        public boolean isSingleValue() {
            return singleValue;
        }

        public boolean isBetweenValue() {
            return betweenValue;
        }

        public boolean isListValue() {
            return listValue;
        }

        public String getTypeHandler() {
            return typeHandler;
        }

        protected Criterion(String condition) {
            super();
            this.condition = condition;
            this.typeHandler = null;
            this.noValue = true;
        }

        protected Criterion(String condition, Object value, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.typeHandler = typeHandler;
            if (value instanceof List<?>) {
                this.listValue = true;
            } else {
                this.singleValue = true;
            }
        }

        protected Criterion(String condition, Object value) {
            this(condition, value, null);
        }

        protected Criterion(String condition, Object value, Object secondValue, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.secondValue = secondValue;
            this.typeHandler = typeHandler;
            this.betweenValue = true;
        }

        protected Criterion(String condition, Object value, Object secondValue) {
            this(condition, value, secondValue, null);
        }
    }
}