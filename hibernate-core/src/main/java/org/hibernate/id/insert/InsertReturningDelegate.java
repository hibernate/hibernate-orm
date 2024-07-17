/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.id.insert;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.values.GeneratedValueBasicResultBuilder;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.generator.values.internal.TableUpdateReturningBuilder;
import org.hibernate.id.PostInsertIdentityPersister;
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

	/**
	 * @deprecated Use {@link #InsertReturningDelegate(EntityPersister, EventType)} instead.
	 */
	@Deprecated( forRemoval = true, since = "6.5" )
	public InsertReturningDelegate(PostInsertIdentityPersister persister, Dialect dialect) {
		this( persister, EventType.INSERT );
	}

	public InsertReturningDelegate(EntityPersister persister, EventType timing) {
		super(
				persister,
				timing,
				true,
				persister.getFactory().getJdbcServices().getDialect().supportsInsertReturningRowId()
		);
		this.tableReference = new MutatingTableReference( persister.getIdentifierTableMapping() );
		final List<GeneratedValueBasicResultBuilder> resultBuilders = jdbcValuesMappingProducer.getResultBuilders();
		this.generatedColumns = new ArrayList<>( resultBuilders.size() );
		for ( GeneratedValueBasicResultBuilder resultBuilder : resultBuilders ) {
			generatedColumns.add( new ColumnReference(
					tableReference,
					getActualGeneratedModelPart( resultBuilder.getModelPart() )
			) );
		}
	}

	@Override
	public TableMutationBuilder<?> createTableMutationBuilder(
			Expectation expectation,
			SessionFactoryImplementor sessionFactory) {
		if ( getTiming() == EventType.INSERT ) {
			return new TableInsertReturningBuilder( persister, tableReference, generatedColumns, sessionFactory );
		}
		else {
			return new TableUpdateReturningBuilder<>( persister, tableReference, generatedColumns, sessionFactory );
		}
	}

	@Override
	protected GeneratedValues executeAndExtractReturning(
			String sql,
			PreparedStatement preparedStatement,
			SharedSessionContractImplementor session) {
		final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
		final JdbcServices jdbcServices = session.getJdbcServices();

		final ResultSet resultSet = jdbcCoordinator.getResultSetReturn().execute( preparedStatement, sql );
		try {
			return getGeneratedValues( preparedStatement, resultSet, persister, getTiming(), session );
		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper().convert(
					e,
					"Unable to extract generated key(s) from generated-keys ResultSet",
					sql
			);
		}
	}

	@Override
	public String prepareIdentifierGeneratingInsert(String insertSQL) {
		return dialect().getIdentityColumnSupport().appendIdentitySelectToInsert(
				( (BasicEntityIdentifierMapping) persister.getRootEntityDescriptor().getIdentifierMapping() ).getSelectionExpression(),
				insertSQL
		);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, SharedSessionContractImplementor session) {
		return session.getJdbcCoordinator().getMutationStatementPreparer().prepareStatement( sql, NO_GENERATED_KEYS );
	}
}
