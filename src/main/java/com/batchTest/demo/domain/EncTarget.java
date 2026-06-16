package com.batchTest.demo.domain;

import lombok.Data;

@Data
public class EncTarget {
    private Long id;
    private String tableName;
    private String columnName;
    private String pkColumn;
    private Long lastPk;
    private String processedYn;
}