/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;

/**
 * @author Rob Worsnop
 */
public class FilterConfiguration {

	// todo (6.0) : this needs re-write to be based on runtime model rather than the boot mode
	//		the only real issue here is the `#getAliasTableMap` code.  That code
	// 		just needs to not be here.  It should be part of creating the runtime model
	//

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
			String table = persistentClass.getMappedTable().getQualifiedTableName().render();
			return Collections.singletonMap( null, table );
		}
		else {
			return Collections.emptyMap();
		}
	}

	private Map<String, String> mergeAliasMaps(SessionFactoryImplementor factory) {
		Map<String, String> ret = new HashMap<>();
		if ( aliasTableMap != null ) {
			ret.putAll( aliasTableMap );
		}
		if ( aliasEntityMap != null ) {
			for ( Map.Entry<String, String> entry : aliasEntityMap.entrySet() ) {
				ret.put(
						entry.getKey(),
						factory.getMetamodel()
								.findEntityDescriptor( entry.getValue() )
								.getPrimaryTable()
								.getTableExpression()
				);
			}
		}
		return ret;
	}
}
