package com.zuul.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.joda.time.LocalDateTime;

import java.math.BigDecimal;

@Data
public class IndexModel {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime time;

    /** 收盘点位 */
    private BigDecimal closingLevel;

    /** 市值 */
    private BigDecimal marketValue;

    /** 流通市值 */
    private BigDecimal marketValueOfCirculation;

    /** PE-TTM(加权平均值) */
    private BigDecimal peTtmWeightAvg;

    /** PE-TTM分位点(加权平均值) */
    private BigDecimal peTtmQuantileWeightAvg;

    /** PE-TTM危险值(加权平均值) */
    private BigDecimal peTtmDangerWeightAvg;

    /** PE-TTM中位值(加权平均值) */
    private BigDecimal peTtmMedianWeightAvg;

    /** PE-TTM机会值(加权平均值) */
    private BigDecimal peTtmOpportunityWeightAvg;
}
