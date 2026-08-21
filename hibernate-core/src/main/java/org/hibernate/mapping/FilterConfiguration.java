/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.internal.SqlFragmentAliasHelper;

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
		final var resolvedAliasTableMap = resolveAliasTableMap( factory );
		if ( !resolvedAliasTableMap.isEmpty() ) {
			return resolvedAliasTableMap;
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

	private Map<String, String> resolveAliasTableMap(SessionFactoryImplementor factory) {
		return SqlFragmentAliasHelper.resolveAliasTableMap( aliasTableMap, aliasEntityMap, factory );
	}
}
