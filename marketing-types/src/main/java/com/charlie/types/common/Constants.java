package com.charlie.types.common;

public class Constants {

    public final static String SPLIT = ",";
    public final static String COLON = ":";
    public final static String SPACE = " ";
    public final static String UNDERLINE = "_";

    public static class RedisKey {
        public static String ACTIVITY_KEY = "big_market_activity_key_";
        public static String ACTIVITY_SKU_KEY = "big_market_activity_sku_key_";
        public static String ACTIVITY_COUNT_KEY = "big_market_activity_count_key_";
        public static String STRATEGY_KEY = "big_market_strategy_key_";
        public static String STRATEGY_AWARD_KEY = "big_market_strategy_award_key_";
        public static String STRATEGY_AWARD_LIST_KEY = "big_market_strategy_award_list_key_";
        public static String STRATEGY_RATE_TABLE_KEY = "big_market_strategy_rate_table_key_";
        public static String STRATEGY_RATE_RANGE_KEY = "big_market_strategy_rate_range_key_";
        public static String RULE_TREE_VO_KEY = "rule_tree_vo_key_";
        public static String STRATEGY_AWARD_COUNT_KEY = "strategy_award_count_key_";
        public static String STRATEGY_AWARD_COUNT_QUERY_KEY = "strategy_award_count_query_key";
        public static String STRATEGY_RULE_WEIGHT_KEY = "strategy_rule_weight_key_";
        public static String ACTIVITY_SKU_COUNT_QUERY_KEY = "activity_sku_count_query_key";
        public static String ACTIVITY_SKU_STOCK_COUNT_KEY = "activity_sku_stock_count_key_";
        public static String ACTIVITY_ACCOUNT_LOCK = "activity_account_lock_";
        public static String USER_CREDIT_ACCOUNT_LOCK = "user_credit_account_lock_";
        public static String ACTIVITY_ACCOUNT_UPDATE_LOCK = "activity_account_update_lock_";

    }

    /**
     * 分布式动态配置中心(DCC)。每个配置项(dataId)与 @DCCValue 中的 key 一一对应，
     * 同一分组下的配置在 Nacos 控制台上统一管理
     */
    public static class DCC {
        public static String CONFIG_GROUP = "marketing-dcc";
    }

}
