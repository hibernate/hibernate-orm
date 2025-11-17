/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.insert;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.generator.values.internal.TableUpdateReturningBuilder;
import org.hibernate.jdbc.Expectation;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.builder.TableMutationBuilder;

import static java.sql.Statement.NO_GENERATED_KEYS;
import static org.hibernate.generator.values.internal.GeneratedValuesHelper.getActualGeneratedModelPart;
import static org.hibernate.generator.values.internal.GeneratedValuesHelper.getGeneratedValues;

/**
 * Delegate for dealing with generated values where the dialect supports
 * returning the generated column directly from the mutation statement.
 * <p>
 * Supports both {@link EventType#INSERT insert} and {@link EventType#UPDATE update} statements.
 *
 * @see org.hibernate.generator.OnExecutionGenerator
 * @see GeneratedValuesMutationDelegate
 */
public class InsertReturningDelegate extends AbstractReturningDelegate {
	private final MutatingTableReference tableReference;
	private final List<ColumnReference> generatedColumns;

	public InsertReturningDelegate(EntityPersister persister, EventType timing) {
		super(
				persister,
				timing,
				true,
				persister.getFactory().getJdbcServices().getDialect()
						.supportsInsertReturningRowId()
		);
		tableReference = new MutatingTableReference( persister.getIdentifierTableMapping() );
		final var resultBuilders = jdbcValuesMappingProducer.getResultBuilders();
		generatedColumns = new ArrayList<>( resultBuilders.size() );
		for ( var resultBuilder : resultBuilders ) {
			generatedColumns.add( new ColumnReference( tableReference,
					getActualGeneratedModelPart( resultBuilder.getModelPart() ) ) );
		}
	}

	@Override
	public TableMutationBuilder<?> createTableMutationBuilder(
			Expectation expectation,
			SessionFactoryImplementor sessionFactory) {
		return getTiming() == EventType.INSERT
				? new TableInsertReturningBuilder( persister, tableReference, generatedColumns, sessionFactory )
				: new TableUpdateReturningBuilder( persister, tableReference, generatedColumns, sessionFactory );
	}

	@Override
	protected GeneratedValues executeAndExtractReturning(
			String sql,
			PreparedStatement preparedStatement,
			SharedSessionContractImplementor session) {
		final ResultSet resultSet =
				session.getJdbcCoordinator().getResultSetReturn()
						.execute( preparedStatement, sql );
		try {
			return getGeneratedValues( resultSet, preparedStatement, persister, getTiming(), session );
		}
		catch (SQLException sqle) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"Unable to extract generated key(s) from generated-keys ResultSet",
					sql
			);
		}
	}

	@Override
	public String prepareIdentifierGeneratingInsert(String insertSQL) {
		final var identifierMapping =
				(BasicEntityIdentifierMapping)
						persister.getRootEntityDescriptor().getIdentifierMapping();
		return dialect().getIdentityColumnSupport()
				.appendIdentitySelectToInsert( identifierMapping.getSelectionExpression(), insertSQL );
	}

	@Override
	public PreparedStatement prepareStatement(String sql, SharedSessionContractImplementor session) {
		return session.getJdbcCoordinator().getMutationStatementPreparer()
				.prepareStatement( sql, NO_GENERATED_KEYS );
	}
}
