/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.id.insert;

import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.internal.CoreLogging;
import org.hibernate.jdbc.Expectation;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.sql.model.ast.builder.TableInsertBuilder;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;
import org.hibernate.generator.OnExecutionGenerator;

/**
 * Delegate for dealing with {@code IDENTITY} columns where the dialect requires an
 * additional command execution to retrieve the generated {@code IDENTITY} value
 */
public class BasicSelectingDelegate extends AbstractSelectingDelegate {
	private final PostInsertIdentityPersister persister;
	private final Dialect dialect;

	public BasicSelectingDelegate(PostInsertIdentityPersister persister, Dialect dialect) {
		super( persister );
		this.persister = persister;
		this.dialect = dialect;
	}

	@Override @Deprecated
	public IdentifierGeneratingInsert prepareIdentifierGeneratingInsert(SqlStringGenerationContext context) {
		IdentifierGeneratingInsert insert = new IdentifierGeneratingInsert( persister.getFactory() );
		insert.addGeneratedColumns( persister.getRootTableKeyColumnNames(), (OnExecutionGenerator) persister.getGenerator() );
		return insert;
	}

	@Override
	public TableInsertBuilder createTableInsertBuilder(
			BasicEntityIdentifierMapping identifierMapping,
			Expectation expectation,
			SessionFactoryImplementor factory) {
		final TableInsertBuilder builder =
				new TableInsertBuilderStandard( persister, persister.getIdentifierTableMapping(), factory );

		final OnExecutionGenerator generator = (OnExecutionGenerator) persister.getGenerator();
		if ( generator.referenceColumnsInSql( dialect ) ) {
			final String[] columnNames = persister.getRootTableKeyColumnNames();
			final String[] columnValues = generator.getReferencedColumnValues( dialect );
			if ( columnValues.length != columnNames.length ) {
				throw new MappingException("wrong number of generated columns");
			}
			for ( int i = 0; i < columnValues.length; i++ ) {
				builder.addKeyColumn( columnNames[i], columnValues[i], identifierMapping.getJdbcMapping() );
			}
		}

		return builder;
	}

	@Override
	protected String getSelectSQL() {
		if ( persister.getIdentitySelectString() == null && !dialect.getIdentityColumnSupport().supportsInsertSelectIdentity() ) {
			throw CoreLogging.messageLogger( BasicSelectingDelegate.class ).nullIdentitySelectString();
		}
		return persister.getIdentitySelectString();
	}
}
