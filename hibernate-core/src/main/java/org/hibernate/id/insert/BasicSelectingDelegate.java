/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.id.insert;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.generator.EventType;
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
		final String identitySelectString = persister.getIdentitySelectString();
		if ( identitySelectString == null
				&& !dialect().getIdentityColumnSupport().supportsInsertSelectIdentity() ) {
			throw new HibernateException( "Cannot retrieve the generated identity, because '"
					+ AvailableSettings.USE_GET_GENERATED_KEYS
					+ "' was disabled and the dialect does not support selecting the last generated identity" );
		}
		return identitySelectString;
	}
}
