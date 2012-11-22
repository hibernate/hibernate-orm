/**
 * Copyright (C) 2007 Google Inc.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */
package org.hibernate.shards.cfg;

/**
 * Describes the configuration properties that can vary across the {@link org.hibernate.SessionFactory}
 * instances contained within your {@link org.hibernate.shards.session.ShardedSessionFactory}.
 *
 * @author maxr@google.com (Max Ross)
 */
public interface ShardConfiguration {

  /**
   * @see org.hibernate.cfg.Environment#URL
   * @return the url of the shard.
   */
  String getShardUrl();

  /**
   * @see org.hibernate.cfg.Environment#USER
   * @return the user that will be sent to the shard for authentication
   */
  String getShardUser();

  /**
   * @see org.hibernate.cfg.Environment#PASS
   * @return the password that will be sent to the shard for authentication
   */
  String getShardPassword();

  /**
   * @return the name that the {@link org.hibernate.SessionFactory} created from
   * this config will have
   */
  String getShardSessionFactoryName();

  /**
   * @return unique id of the shard
   */
  Integer getShardId();

  /**
   * @see org.hibernate.cfg.Environment#DATASOURCE
   * @return the datasource for the shard
   */
  String getShardDatasource();

  /**
   * @see org.hibernate.cfg.Environment#CACHE_REGION_PREFIX
   * @return the cache region prefix for the shard
   */
  String getShardCacheRegionPrefix();
}
