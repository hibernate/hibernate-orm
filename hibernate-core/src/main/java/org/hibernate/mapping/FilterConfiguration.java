/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;

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
		final var mergedAliasTableMap = mergeAliasMaps( factory );
		if ( !mergedAliasTableMap.isEmpty() ) {
			return mergedAliasTableMap;
		}
		else if ( persistentClass != null ) {
			final String tableName =
					persistentClass.getTable()
							.getQualifiedName( factory.getSqlStringGenerationContext() );
			return singletonMap( null, tableName );
		}
		else {
			return emptyMap();
		}
	}

	private Map<String, String> mergeAliasMaps(SessionFactoryImplementor factory) {
		final Map<String, String> result = new HashMap<>();
		if ( aliasTableMap != null ) {
			result.putAll( aliasTableMap );
		}

		if ( aliasEntityMap != null ) {
			for ( var entry : aliasEntityMap.entrySet() ) {
				final var joinable =
						factory.getMappingMetamodel()
								.getEntityDescriptor( entry.getValue() );
				result.put( entry.getKey(), joinable.getTableName() );
			}
		}

		return result;
	}
}
