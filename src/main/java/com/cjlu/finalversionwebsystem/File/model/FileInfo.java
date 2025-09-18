package com.cjlu.finalversionwebsystem.File.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {
    private String name;
    private long size;
    private Date uploadDate;
}
