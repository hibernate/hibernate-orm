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
package org.hibernate.boot.model.relational;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.dialect.Dialect;

/**
 * Convenience base class for {@link org.hibernate.mapping.AuxiliaryDatabaseObject}s.
 * <p/>
 * This implementation performs dialect scoping checks strictly based on
 * dialect name comparisons.  Custom implementations might want to do
 * instanceof-type checks.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractAuxiliaryDatabaseObject
		implements AuxiliaryDatabaseObject, AuxiliaryDatabaseObject.Expandable {
	private static final String EXPORT_IDENTIFIER_PREFIX = "auxiliary-object-";
	private static final AtomicInteger counter = new AtomicInteger( 0 );

	private final String exportIdentifier;
	private final boolean beforeTables;
	private final Set<String> dialectScopes;

	protected AbstractAuxiliaryDatabaseObject() {
		this( null );
	}

	public AbstractAuxiliaryDatabaseObject(boolean beforeTables) {
		this( beforeTables, null );
	}

	protected AbstractAuxiliaryDatabaseObject(Set<String> dialectScopes) {
		this( false, dialectScopes );
	}

	protected AbstractAuxiliaryDatabaseObject(boolean beforeTables, Set<String> dialectScopes) {
		this.beforeTables = beforeTables;
		this.dialectScopes = dialectScopes == null ? new HashSet<String>() : dialectScopes;

		this.exportIdentifier = EXPORT_IDENTIFIER_PREFIX + '.' + counter.getAndIncrement();
	}

	@Override
	public String getExportIdentifier() {
		return exportIdentifier;
	}

	@Override
	public void addDialectScope(String dialectName) {
		dialectScopes.add( dialectName );
	}

	public Set getDialectScopes() {
		return dialectScopes;
	}

	@Override
	public boolean appliesToDialect(Dialect dialect) {
		// empty means no scoping
		return getDialectScopes().isEmpty() || getDialectScopes().contains( dialect.getClass().getName() );
	}

	@Override
	public boolean beforeTablesOnCreation() {
		return beforeTables;
	}
}
