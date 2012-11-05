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

package org.hibernate.shards;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.shards.cfg.ShardConfiguration;
import org.hibernate.shards.cfg.ShardedEnvironment;
import org.hibernate.shards.session.ShardedSessionFactory;
import org.hibernate.shards.session.ShardedSessionFactoryImpl;
import org.hibernate.shards.strategy.ShardStrategyFactory;
import org.hibernate.shards.util.Maps;
import org.hibernate.shards.util.Preconditions;
import org.hibernate.shards.util.Sets;
import org.hibernate.util.PropertiesHelper;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Like regular Hibernate's Configuration, this class helps construct your
 * factories. Not extending Hibernate's Configuration because that is the one place
 * where the notion of a single database is specified (i.e. in the
 * hibernate.properties file). While we would like to maintain the Hibernate paradigm
 * as much as possible, this is one place it might be different.
 *
 * @author Maulik Shah
 */
public class ShardedConfiguration {

  // the prototype config that we'll use when constructing the shard-specific
  // configs
  private final Configuration prototypeConfiguration;

  // shard-specific configs
  private final List<ShardConfiguration> shardConfigs;

  // user-defined sharding behavior
  private final ShardStrategyFactory shardStrategyFactory;

  // maps virtual shard ids to physical shard ids
  private final Map<Integer, Integer> virtualShardToShardMap;

  // maps physical shard ids to sets of virtual shard ids
  private final Map<Integer, Set<ShardId>> shardToVirtualShardIdMap;

  // our lovely logger
  private final Log log = LogFactory.getLog(getClass());

  /**
   * Constructs a ShardedConfiguration.
   *
   * @param prototypeConfiguration The prototype for all shardConfigs that
   * will be used to create the {@link SessionFactory} objects
   * that are internal to the {@link ShardedSessionFactory}.
   * Every {@link org.hibernate.SessionFactory} within the
   * {@link org.hibernate.shards.session.ShardedSessionFactory} objects created by the ShardedConfiguration
   * will look the same, except for properties that we consider to be "variable" (they can
   * vary from shard to shard).  The variable properties are defined by the
   * {@link ShardedConfiguration} interface.
   *
   * @param shardConfigs Shard-specific configuration data for each shard.
   * @param shardStrategyFactory factory that knows how to create the right type of shard strategy
   */
  public ShardedConfiguration(
      Configuration prototypeConfiguration,
      List<ShardConfiguration> shardConfigs,
      ShardStrategyFactory shardStrategyFactory) {
    this(prototypeConfiguration, shardConfigs, shardStrategyFactory, Maps.<Integer, Integer>newHashMap());
  }

  /**
   * Constructs a ShardedConfiguration.
   *
   * @param prototypeConfiguration The prototype for all shardConfigs that
   * will be used to create the {@link org.hibernate.SessionFactory} objects
   * that are internal to the {@link org.hibernate.shards.session.ShardedSessionFactory}.
   * Every {@link org.hibernate.SessionFactory} within the
   * {@link org.hibernate.shards.session.ShardedSessionFactory} objects created by the ShardedConfiguration
   * will look the same, except for properties that we consider to be "variable" (they can
   * vary from shard to shard).  The variable properties are defined by the
   * {@link ShardedConfiguration} interface.
   *
   * @param shardConfigs Shard-specific configuration data for each shard.
   * @param shardStrategyFactory factory that knows how to create the right kind of shard strategy
   * @param virtualShardToShardMap A map that maps virtual shard ids to real
   */
  public ShardedConfiguration(
      Configuration prototypeConfiguration,
      List<ShardConfiguration> shardConfigs,
      ShardStrategyFactory shardStrategyFactory,
      Map<Integer, Integer> virtualShardToShardMap) {
    Preconditions.checkNotNull(prototypeConfiguration);
    Preconditions.checkNotNull(shardConfigs);
    Preconditions.checkArgument(!shardConfigs.isEmpty());
    Preconditions.checkNotNull(shardStrategyFactory);
    Preconditions.checkNotNull(virtualShardToShardMap);

    this.prototypeConfiguration = prototypeConfiguration;
    this.shardConfigs = shardConfigs;
    this.shardStrategyFactory = shardStrategyFactory;
    this.virtualShardToShardMap = virtualShardToShardMap;
    if (!virtualShardToShardMap.isEmpty()) {
      // build the map from shard to set of virtual shards
      shardToVirtualShardIdMap = Maps.newHashMap();
      for(Map.Entry<Integer, Integer> entry : virtualShardToShardMap.entrySet()) {
        Set<ShardId> set = shardToVirtualShardIdMap.get(entry.getValue());
        // see if we already have a set of virtual shards
        if (set == null) {
          // we don't, so create it and add it to the map
          set = Sets.newHashSet();
          shardToVirtualShardIdMap.put(entry.getValue(), set);
        }
        set.add(new ShardId(entry.getKey()));
      }
    } else {
      shardToVirtualShardIdMap = Maps.newHashMap();
    }
  }

  /**
   * @return A ShardedSessionFactory built from the prototype config and
   * the shard-specific configs passed into the constructor.
   */
  public ShardedSessionFactory buildShardedSessionFactory() {
    Map<SessionFactoryImplementor, Set<ShardId>> sessionFactories = Maps.newHashMap();
    // since all configs get their mappings from the prototype config, and we
    // get the set of classes that don't support top-level saves from the mappings,
    // we can get the set from the prototype and then just reuse it.
    Set<Class<?>> classesWithoutTopLevelSaveSupport =
        determineClassesWithoutTopLevelSaveSupport(prototypeConfiguration);
    for (ShardConfiguration config : shardConfigs) {
      populatePrototypeWithVariableProperties(config);
      // get the shardId from the shard-specific config
      Integer shardId = config.getShardId();
      if(shardId == null) {
        final String msg = "Attempt to build a ShardedSessionFactory using a "
            + "ShardConfiguration that has a null shard id.";
        log.fatal(msg);
        throw new NullPointerException(msg);
      }
      Set<ShardId> virtualShardIds;
      if (virtualShardToShardMap.isEmpty()) {
        // simple case, virtual and physical are the same
        virtualShardIds = Collections.singleton(new ShardId(shardId));
      } else {
        // get the set of shard ids that are mapped to the physical shard
        // described by this config
        virtualShardIds = shardToVirtualShardIdMap.get(shardId);
      }
      sessionFactories.put(buildSessionFactory(), virtualShardIds);
    }
    final boolean doFullCrossShardRelationshipChecking =
        PropertiesHelper.getBoolean(
            ShardedEnvironment.CHECK_ALL_ASSOCIATED_OBJECTS_FOR_DIFFERENT_SHARDS,
            prototypeConfiguration.getProperties(),
            true);
    return
        new ShardedSessionFactoryImpl(
            sessionFactories,
            shardStrategyFactory,
            classesWithoutTopLevelSaveSupport,
            doFullCrossShardRelationshipChecking);
  }

  /**
   * @return the Set of mapped classes that don't support top level saves
   */
  @SuppressWarnings("unchecked")
  private Set<Class<?>> determineClassesWithoutTopLevelSaveSupport(Configuration config) {
    Set<Class<?>> classesWithoutTopLevelSaveSupport = Sets.newHashSet();
    for(Iterator<PersistentClass> pcIter = config.getClassMappings(); pcIter.hasNext(); ) {
      PersistentClass pc = pcIter.next();
      for(Iterator<Property> propIter = pc.getPropertyIterator(); propIter.hasNext(); ) {
        if(doesNotSupportTopLevelSave(propIter.next())) {
          Class<?> mappedClass = pc.getMappedClass();
          log.info(String.format("Class %s does not support top-level saves.", mappedClass.getName()));
          classesWithoutTopLevelSaveSupport.add(mappedClass);
          break;
        }
      }
    }
    return classesWithoutTopLevelSaveSupport;
  }

  /**
   * there may be other scenarios, but mappings that contain one-to-one mappings
   * definitely can't be saved as top-level objects (not part of a cascade and
   * no properties from which the shard can be inferred)
   */
  boolean doesNotSupportTopLevelSave(Property property) {
    return property.getValue() != null &&
        OneToOne.class.isAssignableFrom(property.getValue().getClass());
  }

  /**
   * Takes the values of the properties declared in VARIABLE_PROPERTIES from
   * a shard-specific config and sets them as the values of the same properties
   * in the prototype config.
   */
  void populatePrototypeWithVariableProperties(ShardConfiguration config) {
    safeSet(prototypeConfiguration, Environment.USER, config.getShardUser());
    safeSet(prototypeConfiguration, Environment.PASS, config.getShardPassword());
    safeSet(prototypeConfiguration, Environment.URL, config.getShardUrl());
    safeSet(prototypeConfiguration, Environment.DATASOURCE, config.getShardDatasource());
    safeSet(prototypeConfiguration, Environment.CACHE_REGION_PREFIX, config.getShardCacheRegionPrefix());
    safeSet(prototypeConfiguration, Environment.SESSION_FACTORY_NAME, config.getShardSessionFactoryName());
    safeSet(prototypeConfiguration, ShardedEnvironment.SHARD_ID_PROPERTY, config.getShardId().toString());
  }

  /**
   * Set the key to the given value on the given config, but only if the
   * value is not null.
   */
  static void safeSet(Configuration config, String key, String value) {
    if(value != null) {
      config.setProperty(key, value);
    }
  }

  /**
   * Helper function that creates an actual SessionFactory.
   */
  private SessionFactoryImplementor buildSessionFactory() {
    return (SessionFactoryImplementor) prototypeConfiguration.buildSessionFactory();
  }
}
