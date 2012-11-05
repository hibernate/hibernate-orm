/**
 * Copyright (C) 2007 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */
package org.hibernate.shards.example;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;
import org.hibernate.shards.ShardId;
import org.hibernate.shards.ShardedConfiguration;
import org.hibernate.shards.cfg.ConfigurationToShardConfigurationAdapter;
import org.hibernate.shards.cfg.ShardConfiguration;
import org.hibernate.shards.integration.IdGenType;
import org.hibernate.shards.loadbalance.RoundRobinShardLoadBalancer;
import org.hibernate.shards.strategy.ShardStrategy;
import org.hibernate.shards.strategy.ShardStrategyFactory;
import org.hibernate.shards.strategy.ShardStrategyImpl;
import org.hibernate.shards.strategy.access.SequentialShardAccessStrategy;
import org.hibernate.shards.strategy.access.ShardAccessStrategy;
import org.hibernate.shards.strategy.resolution.AllShardsShardResolutionStrategy;
import org.hibernate.shards.strategy.resolution.ShardResolutionStrategy;
import org.hibernate.shards.strategy.selection.RoundRobinShardSelectionStrategy;
import org.hibernate.shards.strategy.selection.ShardSelectionStrategy;
import org.hibernate.shards.util.DatabaseUtils;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This is the sample app we use in the documentation.
 *
 * @author maxr@google.com (Max Ross)
 */
public class WeatherReportApp {

  private SessionFactory sessionFactory;

  public static void main(String[] args) throws Exception {
    WeatherReportApp app = new WeatherReportApp();
    app.run();
  }

  private void run() throws SQLException {
    createSchema();
    sessionFactory = createSessionFactory();

    addData();

    Session session = sessionFactory.openSession();
    try {
      Criteria crit = session.createCriteria(WeatherReport.class);
      List count = crit.list();
      System.out.println(count.size());
      crit.add(Restrictions.gt("temperature", 33));
      List reports = crit.list();
      System.out.println(reports.size());
    } finally {
      session.close();
    }
  }

  private void addData() {
    Session session = sessionFactory.openSession();
    try {
      session.beginTransaction();
      WeatherReport report = new WeatherReport();
      report.setContinent("North America");
      report.setLatitude(new BigDecimal(25));
      report.setLongitude(new BigDecimal(30));
      report.setReportTime(new Date());
      report.setTemperature(44);
      session.save(report);

      report = new WeatherReport();
      report.setContinent("Africa");
      report.setLatitude(new BigDecimal(44));
      report.setLongitude(new BigDecimal(99));
      report.setReportTime(new Date());
      report.setTemperature(31);
      session.save(report);

      report = new WeatherReport();
      report.setContinent("Asia");
      report.setLatitude(new BigDecimal(13));
      report.setLongitude(new BigDecimal(12));
      report.setReportTime(new Date());
      report.setTemperature(104);
      session.save(report);
      session.getTransaction().commit();
    } finally {
      session.close();
    }
  }

  private void createSchema() throws SQLException {
    for(int i = 0; i < 3; i++) {
      DatabaseUtils.destroyDatabase(i, IdGenType.SIMPLE);
      DatabaseUtils.createDatabase(i, IdGenType.SIMPLE);
    }

  }

  public SessionFactory createSessionFactory() {
    Configuration prototypeConfig = new Configuration()
        .configure(getClass().getResource("hibernate0.cfg.xml"));
    prototypeConfig.addURL(getClass().getResource("weather.hbm.xml"));
    List<ShardConfiguration> shardConfigs = new ArrayList<ShardConfiguration>();
    shardConfigs.add(buildShardConfig(getClass().getResource("hibernate0.cfg.xml")));
    shardConfigs.add(buildShardConfig(getClass().getResource("hibernate1.cfg.xml")));
    shardConfigs.add(buildShardConfig(getClass().getResource("hibernate2.cfg.xml")));
    ShardStrategyFactory shardStrategyFactory = buildShardStrategyFactory();
    ShardedConfiguration shardedConfig = new ShardedConfiguration(
        prototypeConfig,
        shardConfigs,
        shardStrategyFactory);
    return shardedConfig.buildShardedSessionFactory();
  }

  ShardStrategyFactory buildShardStrategyFactory() {
    return new ShardStrategyFactory() {
      public ShardStrategy newShardStrategy(List<ShardId> shardIds) {
        RoundRobinShardLoadBalancer loadBalancer
            = new RoundRobinShardLoadBalancer(shardIds);
        ShardSelectionStrategy pss = new RoundRobinShardSelectionStrategy(
            loadBalancer);
        ShardResolutionStrategy prs = new AllShardsShardResolutionStrategy(
            shardIds);
        ShardAccessStrategy pas = new SequentialShardAccessStrategy();
        return new ShardStrategyImpl(pss, prs, pas);
      }
    };
  }

  ShardConfiguration buildShardConfig(URL configFile) {
    Configuration config = new Configuration().configure(configFile);
    return new ConfigurationToShardConfigurationAdapter(config);
  }


}
