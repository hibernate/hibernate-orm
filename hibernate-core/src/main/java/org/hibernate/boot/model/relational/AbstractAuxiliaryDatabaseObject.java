/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.relational;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.dialect.Dialect;

/**
 * Convenience base class for {@link AuxiliaryDatabaseObject}s.
 * <p>
 * This implementation performs dialect scoping checks strictly based on
 * dialect name comparisons.  Custom implementations might want to do
 * instanceof-type checks.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractAuxiliaryDatabaseObject
		implements AuxiliaryDatabaseObject, AuxiliaryDatabaseObject.Expandable {
	private static final String EXPORT_IDENTIFIER_PREFIX = "auxiliary-object-";
	private static final AtomicInteger counter = new AtomicInteger();

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
		this.dialectScopes = dialectScopes == null ? new HashSet<>() : dialectScopes;

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

	public Set<String> getDialectScopes() {
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
