/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Rob Worsnop
 */
public class FilterConfiguration {
	private final String name;
	private final String condition;
	private final boolean autoAliasInjection;
	private final Map<String, String> aliasTableMap;
	private final Map<String, String> aliasEntityMap;
	private final PersistentClass persistentClass;

	public FilterConfiguration(
			String name,
			String condition,
			boolean autoAliasInjection,
			Map<String, String> aliasTableMap,
			Map<String, String> aliasEntityMap,
			PersistentClass persistentClass) {
		this.name = name;
		this.condition = condition;
		this.autoAliasInjection = autoAliasInjection;
		this.aliasTableMap = aliasTableMap;
		this.aliasEntityMap = aliasEntityMap;
		this.persistentClass = persistentClass;
	}

	public String getName() {
		return name;
	}

	public String getCondition() {
		return condition;
	}

	public boolean useAutoAliasInjection() {
		return autoAliasInjection;
	}

	public Map<String, String> getAliasTableMap(SessionFactoryImplementor factory) {
		Map<String, String> mergedAliasTableMap = mergeAliasMaps( factory );
		if ( !mergedAliasTableMap.isEmpty() ) {
			return mergedAliasTableMap;
		}
		else if ( persistentClass != null ) {
			String table = persistentClass.getTable().getQualifiedName(
					factory.getSqlStringGenerationContext()
			);
			return Collections.singletonMap( null, table );
		}
		else {
			return Collections.emptyMap();
		}
	}

	private Map<String, String> mergeAliasMaps(SessionFactoryImplementor factory) {
		final Map<String, String> ret = new HashMap<>();
		if ( aliasTableMap != null ) {
			ret.putAll( aliasTableMap );
		}

		if ( aliasEntityMap != null ) {
			for ( Map.Entry<String, String> entry : aliasEntityMap.entrySet() ) {
				final EntityPersister joinable = factory.getMappingMetamodel()
						.getEntityDescriptor( entry.getValue() );
				ret.put( entry.getKey(), joinable.getTableName() );
			}
		}

		return ret;
	}
}
