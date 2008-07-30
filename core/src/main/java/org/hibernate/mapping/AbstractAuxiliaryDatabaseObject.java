/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.mapping;

import java.util.HashSet;

import org.hibernate.dialect.Dialect;

/**
 * Convenience base class for {@link AuxiliaryDatabaseObject}s.
 * <p/>
 * This implementation performs dialect scoping checks strictly based on
 * dialect name comparisons.  Custom implementations might want to do
 * instanceof-type checks.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractAuxiliaryDatabaseObject implements AuxiliaryDatabaseObject {

	private final HashSet dialectScopes;

	protected AbstractAuxiliaryDatabaseObject() {
		this.dialectScopes = new HashSet();
	}

	protected AbstractAuxiliaryDatabaseObject(HashSet dialectScopes) {
		this.dialectScopes = dialectScopes;
	}

	public void addDialectScope(String dialectName) {
		dialectScopes.add( dialectName );
	}

	public HashSet getDialectScopes() {
		return dialectScopes;
	}

	public boolean appliesToDialect(Dialect dialect) {
		// empty means no scoping
		return dialectScopes.isEmpty() || dialectScopes.contains( dialect.getClass().getName() );
	}
}
