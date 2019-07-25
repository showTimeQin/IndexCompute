package com.zuul.reader;

import com.alibaba.fastjson.JSON;
import com.zuul.model.IndexModel;
import org.joda.time.LocalDateTime;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ParseBigDecimal;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.cellprocessor.joda.ParseLocalDateTime;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.prefs.CsvPreference;

import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CsvReader {

    private final static Integer HALF_YEAR = 125;
    private final static DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * An example of reading using CsvBeanReader.
     */
    public static void readWithCsvBeanReader(String dir) throws Exception {

        List<String> filePath = getAllFile(dir);


        ICsvBeanReader beanReader = null;
        for (String fileName : filePath) {

            //把数据存入List
            List<IndexModel> allIndexModel = new ArrayList<>();
            beanReader = new CsvBeanReader(new FileReader(fileName), CsvPreference.STANDARD_PREFERENCE);

            // the header elements are used to map the values to the bean (names must match)
            String[] header = beanReader.getHeader(true);
            header = new String[]{"time", "closingLevel", "marketValue", "marketValueOfCirculation", "peTtmWeightAvg", "peTtmQuantileWeightAvg", "peTtmDangerWeightAvg", "peTtmMedianWeightAvg", "peTtmOpportunityWeightAvg"};
            final CellProcessor[] processors = getIndexModelProcessors();



            while (true) {
                IndexModel indexModel = null;
                try {
                    indexModel = beanReader.read(IndexModel.class, header, processors);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (indexModel == null) {
                    break;
                }
                allIndexModel.add(indexModel);
            }


            Collections.reverse(allIndexModel);

            //维护一个125天(半年)的队列
            Queue<IndexModel> halfYear = new ConcurrentLinkedQueue<IndexModel>();
            //维护一个峰
            BigDecimal feng = null;
            //维护最高峰
            BigDecimal topFeng = null;
            //维护一个趋势
            boolean up = false;
            //维护前1个值
            BigDecimal lastValue = null;
            //维护前2个值
            BigDecimal lastlastValue = null;
            //观察买入
            boolean viewBuy = false;
            //观察买入,进入谷之后的标志
            boolean viewBuyPlus = false;
            //买入
            boolean buy = false;
            //持有
            boolean has = false;
            //观察卖出
            boolean viewSell = false;
            //卖出
            boolean sell = false;

            //买入时间
            LocalDateTime buyTime = LocalDateTime.now();

            //买入时间
            LocalDateTime sellTime = LocalDateTime.now();

            //赚取点位
            BigDecimal income = new BigDecimal(0);

            for (IndexModel indexModel : allIndexModel) {

                halfYear.offer(JSON.parseObject(JSON.toJSONString(indexModel), IndexModel.class));
                //获取125天前的值
                if (halfYear.size() > HALF_YEAR) {
                    IndexModel poll = halfYear.poll();
                    if (feng.compareTo(poll.getPeTtmWeightAvg()) < 0) {
                        feng = poll.getPeTtmWeightAvg();
                    }
                }
                //初始化
                if (lastValue == null) {
                    lastValue = indexModel.getPeTtmWeightAvg();
                }
                //买入
                if (buy) {
                    buyTime = indexModel.getTime();
                    income = indexModel.getPeTtmWeightAvg();
                    buy = false;
                    has = true;
                }
                //持有资产，观察卖出
                if (has) {
                    if (sell) {
                        sellTime = indexModel.getTime();
                        income = indexModel.getPeTtmWeightAvg().subtract(income);
                        System.out.println(fileName + "：    买入时间：" + buyTime.toString("yyyy-MM-dd") + "   卖出时间:" + sellTime.toString("yyyy-MM-dd") + "   收入：" + income.toString());
                        income = new BigDecimal(0);
                        sell = false;
                        has = false;
                    }
                    if (!up) {
                        //当前值小于之前值时，为下降趋势
                        if (lastValue.compareTo(indexModel.getPeTtmWeightAvg()) > 0) {
                            //两次下跌,观察卖出
                            up = false;
                            viewSell = true;
                        } else {
                            up = true;
                        }
                    } else {
                        //当前值小于最高峰
                        if (indexModel.getPeTtmWeightAvg().compareTo(feng) < 0) {
                            //无法冲破最高峰,前一天为上升趋势，当前值小于之前值时，为下降趋势
                            if (lastValue.compareTo(indexModel.getPeTtmWeightAvg()) > 0) {
                                up = false;
                                if (viewSell) {
                                    sell = true;
                                    viewSell = false;
                                }
                            }
                        }
                    }

                }
                //没持有资产，观察买入
                if (!has) {
                    //当前值大于之前值时，为上升趋势
                    if (lastValue.compareTo(indexModel.getPeTtmWeightAvg()) < 0) {
                        //之前为下降趋势，则为谷,之后的值高于峰时买入
                        if (!up) {
                            if (viewBuy) {
                                viewBuyPlus = true;
                            }
                        }

                        if (viewBuyPlus) {
                            //当前峰再次大于历史高峰
                            if (indexModel.getPeTtmWeightAvg().compareTo(feng) < 0) {
                                //判断前一天是否在历史高峰±0.5
                                if (Math.abs(lastValue.subtract(feng).doubleValue()) < 0.5) {
                                    buy = true;
                                    viewBuy = false;
                                    viewBuyPlus = false;
                                }
                            }
                        }
                        up = true;
                    } else {
                        //当之前为上升趋势时，形成一个峰
                        if (up) {
                            //峰初始化
                            if (feng == null) {
                                feng = lastValue;
                                topFeng = feng;
                            } else {
                                if (feng.compareTo(lastValue) < 0) { //当前峰大于历史高峰
                                    viewBuy = true;
                                }
                            }
                        }
                        up = false;
                    }
                }

                lastlastValue = lastValue;
                lastValue = indexModel.getPeTtmWeightAvg();
            }
        }


    }

    private static CellProcessor[] getIndexModelProcessors() {
        return new CellProcessor[]{
                new ParseLocalDateTime(),
                new Optional(new ParseBigDecimal()),
                new Optional(new ParseBigDecimal()),
                new Optional(new ParseBigDecimal()),
                new Optional(new ParseBigDecimal()),
                new Optional(new ParseBigDecimal()),
                new Optional(new ParseBigDecimal()),
                new Optional(new ParseBigDecimal()),
                new Optional(new ParseBigDecimal())
        };
    }

    //pathname参数表示自己制定的文件路径的
    private static List<String> getAllFile(String pathname) {
        //先将指定路径下的所有文件实例化
        File file = new File(pathname);
        //判断实例化的对象file是否存在，即指定路径是否存在
        if (!file.exists()) {
            //若file不存在，则抛出异常
            throw new IllegalArgumentException("目录" + pathname + "不存在");
        }
        //若文件存在，则将所有文件的实例化对象转化为数组形式
        List<String> paths = new ArrayList<String>();
        File[] files = file.listFiles();
        //遍历文件数组
        assert files != null;
        for (File file2 : files) {
            //如果是拿出来的File是文件夹类型，就调用自己，利用递归的思想，即一层一层地打开
            if (file2.isDirectory()) {
                //调用自己时候传入的参数为上一句判断出来的文件夹路径
                paths.addAll(getAllFile(file2.getAbsolutePath()));
            } else {
                //如果从数组中拿出来的值是File是文件类型，就直接先打印这个文件的路径名称
                paths.add(file2.getPath());
            }
        }
        return paths;
    }

}