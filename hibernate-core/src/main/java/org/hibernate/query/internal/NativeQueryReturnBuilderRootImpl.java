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
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.query.NativeQuery;

/**
 * @author Steve Ebersole
 */
public class NativeQueryReturnBuilderRootImpl implements NativeQuery.RootReturn, NativeQueryReturnBuilder {
	private final String alias;
	private final String entityName;
	private LockMode lockMode = LockMode.READ;
	private Map<String, String[]> propertyMappings;

	public NativeQueryReturnBuilderRootImpl(String alias, String entityName) {
		this.alias = alias;
		this.entityName = entityName;
	}

	@Override
	public NativeQuery.RootReturn setLockMode(LockMode lockMode) {
		this.lockMode = lockMode;
		return this;
	}

	@Override
	public NativeQuery.RootReturn setDiscriminatorAlias(String alias) {
		addProperty( "class", alias );
		return this;
	}

	@Override
	public NativeQuery.RootReturn addProperty(String propertyName, String columnAlias) {
		addProperty( propertyName ).addColumnAlias( columnAlias );
		return this;
	}

	@Override
	public NativeQuery.ReturnProperty addProperty(final String propertyName) {
		if ( propertyMappings == null ) {
			propertyMappings = new HashMap<>();
		}
		return new NativeQuery.ReturnProperty() {
			@Override
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

	@Override
	public NativeSQLQueryReturn buildReturn() {
		return new NativeSQLQueryRootReturn( alias, entityName, propertyMappings, lockMode );
	}
}
