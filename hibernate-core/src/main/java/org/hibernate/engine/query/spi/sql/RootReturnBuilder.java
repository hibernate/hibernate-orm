/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.engine.query.spi.sql;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.LockMode;
import org.hibernate.SQLQuery;

/**
* @author Strong Liu <stliu@hibernate.org>
*/
public class RootReturnBuilder implements SQLQuery.RootReturn, ReturnBuilder {
	private final String alias;
	private final String entityName;
	private LockMode lockMode = LockMode.READ;
	private Map<String,String[]> propertyMappings;

	public RootReturnBuilder(String alias, String entityName) {
		this.alias = alias;
		this.entityName = entityName;
	}
	@Override
	public SQLQuery.RootReturn setLockMode(LockMode lockMode) {
		this.lockMode = lockMode;
		return this;
	}
	@Override
	public SQLQuery.RootReturn setDiscriminatorAlias(String alias) {
		addProperty( "class", alias );
		return this;
	}
	@Override
	public SQLQuery.RootReturn addProperty(String propertyName, String columnAlias) {
		addProperty( propertyName ).addColumnAlias( columnAlias );
		return this;
	}
	@Override
	public SQLQuery.ReturnProperty addProperty(final String propertyName) {
		if ( propertyMappings == null ) {
			propertyMappings = new HashMap<String,String[]>();
		}
		return new SQLQuery.ReturnProperty() {
			@Override
			public SQLQuery.ReturnProperty addColumnAlias(String columnAlias) {
				String[] columnAliases = propertyMappings.get( propertyName );
				if ( columnAliases == null ) {
					columnAliases = new String[]{columnAlias};
				}else{
					String[] newColumnAliases = new String[columnAliases.length + 1];
					System.arraycopy( columnAliases, 0, newColumnAliases, 0, columnAliases.length );
					newColumnAliases[columnAliases.length] = columnAlias;
					columnAliases = newColumnAliases;
				}
				propertyMappings.put( propertyName,columnAliases );
				return this;
			}
		};
	}
	@Override
	public NativeSQLQueryReturn buildReturn() {
		return new NativeSQLQueryRootReturn( alias, entityName, propertyMappings, lockMode );
	}
}
