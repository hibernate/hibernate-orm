/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.LockMode;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryJoinReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.query.NativeQuery;

/**
 * @author Steve Ebersole
 */
public class NativeQueryReturnBuilderFetchImpl implements NativeQuery.FetchReturn, NativeQueryReturnBuilder {
	private final String alias;
	private String ownerTableAlias;
	private final String joinedPropertyName;
	private LockMode lockMode = LockMode.READ;
	private Map<String, String[]> propertyMappings;

	public NativeQueryReturnBuilderFetchImpl(String alias, String ownerTableAlias, String joinedPropertyName) {
		this.alias = alias;
		this.ownerTableAlias = ownerTableAlias;
		this.joinedPropertyName = joinedPropertyName;
	}

	public NativeQuery.FetchReturn setLockMode(LockMode lockMode) {
		this.lockMode = lockMode;
		return this;
	}

	public NativeQuery.FetchReturn addProperty(String propertyName, String columnAlias) {
		addProperty( propertyName ).addColumnAlias( columnAlias );
		return this;
	}

	public NativeQuery.ReturnProperty addProperty(final String propertyName) {
		if ( propertyMappings == null ) {
			propertyMappings = new HashMap<>();
		}
		return new NativeQuery.ReturnProperty() {
			public NativeQuery.ReturnProperty addColumnAlias(String columnAlias) {
				String[] columnAliases = propertyMappings.get( propertyName );
				if ( columnAliases == null ) {
					columnAliases = new String[] {columnAlias};
				}
				else {
					String[] newColumnAliases = new String[columnAliases.length + 1];
					System.arraycopy( columnAliases, 0, newColumnAliases, 0, columnAliases.length );
					newColumnAliases[columnAliases.length] = columnAlias;
					columnAliases = newColumnAliases;
				}
				propertyMappings.put( propertyName, columnAliases );
				return this;
			}
		};
	}

	public NativeSQLQueryReturn buildReturn() {
		return new NativeSQLQueryJoinReturn(
				alias,
				ownerTableAlias,
				joinedPropertyName,
				propertyMappings,
				lockMode
		);
	}
}
