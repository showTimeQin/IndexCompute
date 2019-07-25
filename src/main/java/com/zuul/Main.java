package com.zuul;

import com.zuul.reader.CsvReader;

public class Main {

    public static void main(String[] args) throws Exception {
        CsvReader.readWithCsvBeanReader("E:\\神秘の地\\股票\\指数\\PE_TTM指数3年数据");
    }
}

