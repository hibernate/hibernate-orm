/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.id.insert;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.internal.CoreLogging;
import org.hibernate.jdbc.Expectation;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;
import org.hibernate.sql.model.ast.builder.TableMutationBuilder;

/**
 * Delegate for dealing with {@code IDENTITY} columns where the dialect requires an
 * additional command execution to retrieve the generated {@code IDENTITY} value
 */
public class BasicSelectingDelegate extends AbstractSelectingDelegate {
	final private EntityPersister persister;

	/**
	 * @deprecated Use {@link #BasicSelectingDelegate(EntityPersister)} instead.
	 */
	@Deprecated( forRemoval = true, since = "6.5" )
	public BasicSelectingDelegate(PostInsertIdentityPersister persister, Dialect dialect) {
		this( persister );
	}

	public BasicSelectingDelegate(EntityPersister persister) {
		super( persister, EventType.INSERT, false, false );
		this.persister = persister;
	}

	@Override
	public TableMutationBuilder<?> createTableMutationBuilder(
			Expectation expectation,
			SessionFactoryImplementor factory) {
		return new TableInsertBuilderStandard( persister, persister.getIdentifierTableMapping(), factory );
	}

	@Override
	protected String getSelectSQL() {
		if ( persister.getIdentitySelectString() == null && !dialect().getIdentityColumnSupport().supportsInsertSelectIdentity() ) {
			throw CoreLogging.messageLogger( BasicSelectingDelegate.class ).nullIdentitySelectString();
		}
		return persister.getIdentitySelectString();
	}
}
