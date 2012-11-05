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
package org.hibernate.shards.cfg;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

/**
 * Adapt a {@link Configuration} to the {@link ShardConfiguration} interface.
 *
 * @author maxr@google.com (Max Ross)
 */
public class ConfigurationToShardConfigurationAdapter implements ShardConfiguration {

  private final Configuration config;

  public ConfigurationToShardConfigurationAdapter(Configuration config) {
    this.config = config;
  }

  public String getShardUrl() {
    return config.getProperty(Environment.URL);
  }

  public String getShardUser() {
    return config.getProperty(Environment.USER);
  }

  public String getShardPassword() {
    return config.getProperty(Environment.PASS);
  }

  public String getShardSessionFactoryName() {
    return config.getProperty(Environment.SESSION_FACTORY_NAME);
  }

  public Integer getShardId() {
    return Integer.parseInt(config.getProperty(ShardedEnvironment.SHARD_ID_PROPERTY));
  }

  public String getShardDatasource() {
    return config.getProperty(Environment.DATASOURCE);
  }

  public String getShardCacheRegionPrefix() {
    return config.getProperty(Environment.CACHE_REGION_PREFIX);
  }
}
