/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;

/**
 * @author Rob Worsnop
 */
public class FilterConfiguration implements Serializable {
	private final String name;
	private final String condition;
	private final boolean autoAliasInjection;
	private final Map<String, String> aliasTableMap;
	private final Map<String, String> aliasEntityMap;
	private final QualifiedTableName persistentClassTableName;
	private final String persistentClassSubselect;

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
		this.persistentClassTableName =
				persistentClass == null ? null : persistentClass.getTable().getQualifiedTableName();
		this.persistentClassSubselect =
				persistentClass == null ? null : persistentClass.getTable().getSubselect();
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
		return getAliasTableMap( factory.getMappingMetamodel(), factory.getSqlStringGenerationContext() );
	}

	public Map<String, String> getAliasTableMap(
			MappingMetamodelImplementor mappingMetamodel,
			SqlStringGenerationContext sqlStringGenerationContext) {
		final var mergedAliasTableMap = mergeAliasMaps( mappingMetamodel );
		if ( !mergedAliasTableMap.isEmpty() ) {
			return mergedAliasTableMap;
		}
		else if ( persistentClassSubselect != null ) {
			return singletonMap( null, "( " + persistentClassSubselect + " )" );
		}
		else if ( persistentClassTableName != null ) {
			final String tableName =
					sqlStringGenerationContext.format( persistentClassTableName );
			return singletonMap( null, tableName );
		}
		else {
			return emptyMap();
		}
	}

	private Map<String, String> mergeAliasMaps(MappingMetamodelImplementor mappingMetamodel) {
		final Map<String, String> result = new HashMap<>();
		if ( aliasTableMap != null ) {
			result.putAll( aliasTableMap );
		}

		if ( aliasEntityMap != null ) {
			for ( var entry : aliasEntityMap.entrySet() ) {
				final var joinable =
						mappingMetamodel
								.getEntityDescriptor( entry.getValue() );
				result.put( entry.getKey(), joinable.getTableName() );
			}
		}

		return result;
	}
}
