package com.msopentech;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by v-wajie on 2015/11/2.
 */
public class LoggerBolt extends BaseRichBolt{

    private OutputCollector collector;
    private static final Logger logger =
            LoggerFactory.getLogger(LoggerBolt.class);

    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        collector = outputCollector;
    }

    public void execute(Tuple tuple) {
        String value = tuple.getString(0);
        logger.info("Tuple Value: " + value);

        collector.ack(tuple);
    }

    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {

    }
}
