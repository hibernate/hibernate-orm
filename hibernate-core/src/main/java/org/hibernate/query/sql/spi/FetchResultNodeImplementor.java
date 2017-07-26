/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.spi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.LockMode;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.sql.AttributeResultRegistration;
import org.hibernate.query.sql.FetchResultRegistration;
import org.hibernate.sql.ast.tree.internal.select.FetchEntityAttributeImpl;
import org.hibernate.sql.ast.tree.spi.select.Fetch;

/**
 * @author Steve Ebersole
 */
public class FetchResultNodeImplementor implements NativeQuery.FetchReturn, FetchResultRegistration {
	private final String alias;
	private String ownerTableAlias;
	private final String joinedPropertyName;
	private LockMode lockMode = LockMode.READ;
	private Map<String, String[]> propertyMappings;

	public FetchResultNodeImplementor(String alias, String ownerTableAlias, String joinedPropertyName) {
		this.alias = alias;
		this.ownerTableAlias = ownerTableAlias;
		this.joinedPropertyName = joinedPropertyName;
	}

	@Override
	public LockMode getLockMode() {
		return lockMode;
	}

	@Override
	public List<AttributeResultRegistration> getAttributeResultRegistrations() {
		return null;
	}

	@Override
	public NativeQuery.FetchReturn setLockMode(LockMode lockMode) {
		this.lockMode = lockMode;
		return this;
	}

	@Override
	public NativeQuery.FetchReturn addProperty(String propertyName, String columnAlias) {
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



//	public Fetch buildFetch() {
//		// todo (6.0) - going to have to pass in the FetchParent, SqlSelectionResolver and others
//		return new FetchEntityAttributeImpl(  );
//	}

//	public NativeSQLQueryReturn buildReturn() {
//		return new NativeSQLQueryJoinReturn(
//				alias,
//				ownerTableAlias,
//				joinedPropertyName,
//				propertyMappings,
//				lockMode
//		);
//	}
}
