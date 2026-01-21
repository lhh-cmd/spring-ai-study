package com.ai.haha.springaistudyservice.service.moyoo.enums;

/**
 * 职业枚举
 */
public enum Profession {
    PROGRAMMER("程序员", "developer"),
    DESIGNER("设计师", "designer"),
    PRODUCT_MANAGER("产品经理", "product manager"),
    TESTER("测试工程师", "tester"),
    OPERATION("运营", "operation"),
    MARKETING("市场", "marketing"),
    SALES("销售", "sales"),
    HR("人力资源", "hr"),
    FINANCE("财务", "finance"),
    OTHER("其他", "other");
    
    private final String chineseName;
    private final String englishName;
    
    Profession(String chineseName, String englishName) {
        this.chineseName = chineseName;
        this.englishName = englishName;
    }
    
    public String getChineseName() {
        return chineseName;
    }
    
    public String getEnglishName() {
        return englishName;
    }
    
    /**
     * 根据中文名称获取枚举
     */
    public static Profession fromChineseName(String chineseName) {
        for (Profession profession : values()) {
            if (profession.chineseName.equals(chineseName)) {
                return profession;
            }
        }
        return OTHER;
    }
    
    /**
     * 根据英文名称获取枚举
     */
    public static Profession fromEnglishName(String englishName) {
        for (Profession profession : values()) {
            if (profession.englishName.equalsIgnoreCase(englishName)) {
                return profession;
            }
        }
        return OTHER;
    }
}

