package com.datadrift.model.change;

import lombok.Data;

@Data
public class ColumnValue {
    private String name;
    private String value;
    private String valueType;  // STRING, NUMERIC, BOOLEAN, NULL, TIMESTAMP, DATE
}
