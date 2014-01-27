/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.persister.entity.Joinable;

/**
 * Captures all relevant metadata specified as part of defining the filter.
 *
 * @author Rob Worsnop
 */
public class FilterConfiguration {
	private final String name;
	private final String condition;
	private final boolean autoAliasInjection;
	private final Map<String, String> aliasTableMap;
	private final Map<String, String> aliasEntityMap;
	private final PersistentClass persistentClass;
	private final EntityBinding entityBinding;
	public FilterConfiguration(
			String name,
			String condition,
			boolean autoAliasInjection,
			Map<String, String> aliasTableMap,
			Map<String, String> aliasEntityMap){
		this.name = name;
		this.condition = condition;
		this.autoAliasInjection = autoAliasInjection;
		this.aliasTableMap = aliasTableMap;
		this.aliasEntityMap = aliasEntityMap;
		this.entityBinding = null;
		this.persistentClass = null;
	}

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
		this.entityBinding = null;
	}

	public FilterConfiguration(
			String name,
			String condition,
			boolean autoAliasInjection,
			Map<String, String> aliasTableMap,
			Map<String, String> aliasEntityMap,
			EntityBinding entityBinding) {
		this.name = name;
		this.condition = condition;
		this.autoAliasInjection = autoAliasInjection;
		this.aliasTableMap = aliasTableMap;
		this.aliasEntityMap = aliasEntityMap;
		this.persistentClass = null;
		this.entityBinding = entityBinding;
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
					factory.getDialect(),
					factory.getSettings().getDefaultCatalogName(),
					factory.getSettings().getDefaultSchemaName()
			);
			return Collections.singletonMap( null, table );
		}
		else if ( entityBinding != null ) {
			String table = entityBinding.getPrimaryTable().getQualifiedName( factory.getDialect() );
			return Collections.singletonMap( null, table );
		}
		else {
			return Collections.emptyMap();
		}
	}

	private Map<String, String> mergeAliasMaps(SessionFactoryImplementor factory) {
		Map<String, String> ret = new HashMap<String, String>();
		if ( aliasTableMap != null ) {
			ret.putAll( aliasTableMap );
		}
		if ( aliasEntityMap != null ) {
			for ( Map.Entry<String, String> entry : aliasEntityMap.entrySet() ) {
				ret.put(
						entry.getKey(),
						Joinable.class.cast( factory.getEntityPersister( entry.getValue() ) ).getTableName()
				);
			}
		}
		return ret;
	}
}
