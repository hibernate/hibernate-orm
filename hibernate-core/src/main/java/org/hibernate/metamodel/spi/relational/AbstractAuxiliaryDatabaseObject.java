/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.spi.relational;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
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
public abstract class AbstractAuxiliaryDatabaseObject implements AuxiliaryDatabaseObject {
	// Use a UUID in identifier prefix because this object is not qualified by a schema/catalog
	// (not sure this matters...)
	private static final String EXPORT_IDENTIFIER_PREFIX = "auxiliary-object-" + UUID.randomUUID();
	private static final AtomicInteger counter = new AtomicInteger( 0 );
	private final String exportIdentifier;
	private final Set<String> dialectScopes;
	private boolean beforeTablesOnCreation;

	protected AbstractAuxiliaryDatabaseObject(Set<String> dialectScopes) {
		this( dialectScopes, false );
	}

	protected AbstractAuxiliaryDatabaseObject(Set<String> dialectScopes, boolean beforeTablesOnCreation) {
		this.dialectScopes =  dialectScopes == null ? new HashSet<String>() : dialectScopes;
		this.beforeTablesOnCreation = beforeTablesOnCreation;
		this.exportIdentifier =
				new StringBuilder( EXPORT_IDENTIFIER_PREFIX )
						.append( '.' )
						.append( counter.getAndIncrement() )
						.toString();
	}

	public void addDialectScope(String dialectName) {
		dialectScopes.add( dialectName );
	}

	public Iterable<String> getDialectScopes() {
		return dialectScopes;
	}

	public boolean appliesToDialect(Dialect dialect) {
		// empty means no scoping
		return dialectScopes.isEmpty() || dialectScopes.contains( dialect.getClass().getName() );
	}

	@Override
	public String getExportIdentifier() {
		return exportIdentifier;
	}

	@Override
	public boolean beforeTablesOnCreation() {
		return beforeTablesOnCreation;
	}
}
