package com.msopentech;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.AuthorizationException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.generated.StormTopology;
import backtype.storm.topology.TopologyBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.microsoft.eventhubs.spout.EventHubSpout;
import com.microsoft.eventhubs.spout.EventHubSpoutConfig;
import org.apache.storm.jdbc.bolt.JdbcInsertBolt;
import org.apache.storm.jdbc.common.Column;
import org.apache.storm.jdbc.common.ConnectionProvider;
import org.apache.storm.jdbc.common.HikariCPConnectionProvider;
import org.apache.storm.jdbc.mapper.JdbcMapper;
import org.apache.storm.jdbc.mapper.SimpleJdbcMapper;

import java.io.IOException;
import java.sql.Types;
import java.util.List;
import java.util.Map;

/**
 * Created by v-wajie on 2015/11/2.
 */
public class LogTopology {
    protected EventHubSpoutConfig spoutConfig;
    protected int numWorkers;
    private ConnectionProvider connectionProvider;
    private JdbcMapper jdbcMapper;
    private static final String JDBC_CONF = "jdbc.conf";

    private void initConfig(String[] args) {

        String username = "Receiving";
        String password = "mkHgBIhNMLoyLxyF/vlO1CefguSLU14b2H0PJTo3/cU=";
        String namespaceName = "openiot";
        String entityPath = "xixiandevice";

        int partitionCount = 4;

        spoutConfig = new EventHubSpoutConfig(username, password, namespaceName,
                entityPath, partitionCount);
        spoutConfig.setTargetAddress("servicebus.chinacloudapi.cn");

        numWorkers = spoutConfig.getPartitionCount();

        if (args.length > 0) {
            //set Topology name so that sample Trident topology can use it as stream naem
            spoutConfig.setTopologyName(args[0]);
        }
    }

    private void initJDBCConnection() {


    }

    protected StormTopology buildTopology(JdbcInsertBolt insertBolt) {

        TopologyBuilder builder = new TopologyBuilder();

        EventHubSpout eventHubSpout = new EventHubSpout(spoutConfig);
        builder.setSpout("EventHubSpout", eventHubSpout, spoutConfig.getPartitionCount())
                .setNumTasks(spoutConfig.getPartitionCount());
        builder.setBolt("LoggerBolt", new LoggerBolt(), spoutConfig.getPartitionCount())
                .localOrShuffleGrouping("EventHubSpout")
                .setNumTasks(spoutConfig.getPartitionCount());
        builder.setBolt("SqlInsertBolt", insertBolt, spoutConfig.getPartitionCount())
                .localOrShuffleGrouping("EventHubSpout")
                .setNumTasks(spoutConfig.getPartitionCount());

        return builder.createTopology();
    }

    protected void runScenario(String[] args) throws IOException,
            InterruptedException, AlreadyAliveException, InvalidTopologyException, AuthorizationException {
        boolean runLocal = true;
        //readEHConfig(args);
        initConfig(args);
        Map map = null;
        try {
            map = Maps.newHashMap();
            map.put("dataSourceClassName", "com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
            map.put("dataSource.url", "jdbc:mysql://localhost:3306/openiot");
            map.put("dataSource.user", "root");
            map.put("dataSource.password", "");
        } catch (Exception e) {
            e.printStackTrace();
        }

        connectionProvider = new HikariCPConnectionProvider(map);

        List<Column> schemaColums = Lists.newArrayList(new Column("message", Types.VARCHAR));
        jdbcMapper = new SimpleJdbcMapper(schemaColums);

        JdbcInsertBolt insertBolt = new JdbcInsertBolt(connectionProvider, jdbcMapper)
                .withInsertQuery("insert into device (tuple) values (?)");

        StormTopology topology = buildTopology(insertBolt);
        Config config = new Config();
        config.put(JDBC_CONF, map);
        //config.setDebug(true);

        if (runLocal) {
            config.setMaxTaskParallelism(3);
            LocalCluster localCluster = new LocalCluster();
            localCluster.submitTopology("test", config, topology);
            Thread.sleep(5000000);
            localCluster.shutdown();
        } else {
            config.setNumWorkers(numWorkers);
            config.setDebug(true);
            StormSubmitter.submitTopology(args[0], config, topology);
        }
    }

    public static void main(String[] args) throws InterruptedException,
            AlreadyAliveException, InvalidTopologyException, IOException, AuthorizationException {
        LogTopology logTopology = new LogTopology();
        logTopology.runScenario(args);
    }
}
