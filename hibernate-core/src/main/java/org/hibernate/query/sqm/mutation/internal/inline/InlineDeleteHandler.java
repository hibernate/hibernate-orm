/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.inline;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.MutableInteger;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmJdbcExecutionContextAdapter;
import org.hibernate.query.sqm.mutation.internal.DeleteHandler;
import org.hibernate.query.sqm.mutation.internal.MatchingIdSelectionHelper;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcDelete;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.StatementCreatorHelper;

/**
 * DeleteHandler for the in-line strategy
 *
 * @author Evandro Pires da Silva
 * @author Vlad Mihalcea
 * @author Steve Ebersole
 */
public class InlineDeleteHandler implements DeleteHandler {
	private final MatchingIdRestrictionProducer matchingIdsPredicateProducer;
	private final SqmDeleteStatement sqmDeleteStatement;
	private final DomainParameterXref domainParameterXref;

	private final DomainQueryExecutionContext executionContext;

	private final SessionFactoryImplementor sessionFactory;
	private final SqlAstTranslatorFactory sqlAstTranslatorFactory;
	private final JdbcMutationExecutor jdbcMutationExecutor;

	protected InlineDeleteHandler(
			MatchingIdRestrictionProducer matchingIdsPredicateProducer,
			SqmDeleteStatement sqmDeleteStatement,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext context) {
		this.sqmDeleteStatement = sqmDeleteStatement;

		this.domainParameterXref = domainParameterXref;
		this.matchingIdsPredicateProducer = matchingIdsPredicateProducer;

		this.executionContext = context;

		this.sessionFactory = executionContext.getSession().getFactory();
		this.sqlAstTranslatorFactory = sessionFactory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory();
		this.jdbcMutationExecutor = sessionFactory.getJdbcServices().getJdbcMutationExecutor();
	}

	@Override
	public int execute(DomainQueryExecutionContext executionContext) {
		final List<Object> idsAndFks = MatchingIdSelectionHelper.selectMatchingIds(
				sqmDeleteStatement,
				domainParameterXref,
				executionContext
		);

		if ( idsAndFks == null || idsAndFks.isEmpty() ) {
			return 0;
		}

		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();

		final String mutatingEntityName = sqmDeleteStatement.getTarget().getModel().getHibernateEntityName();
		final EntityMappingType entityDescriptor = factory.getDomainModel().getEntityDescriptor( mutatingEntityName );

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl( domainParameterXref.getQueryParameterCount() );

		// delete from the tables
		final MutableInteger valueIndexCounter = new MutableInteger();
		entityDescriptor.visitSubTypeAttributeMappings(
				attribute -> {
					if ( attribute instanceof PluralAttributeMapping ) {
						final PluralAttributeMapping pluralAttribute = (PluralAttributeMapping) attribute;

						if ( pluralAttribute.getSeparateCollectionTable() != null ) {
							// this collection has a separate collection table, meaning it is one of:
							//		1) element-collection
							//		2) many-to-many
							//		3) one-to many using a dedicated join-table
							//
							// in all of these cases, we should clean up the matching rows in the
							// collection table
							final ModelPart fkTargetPart = pluralAttribute.getKeyDescriptor().getTargetPart();
							final int valueIndex;
							if ( fkTargetPart instanceof EntityIdentifierMapping ) {
								valueIndex = 0;
							}
							else {
								if ( valueIndexCounter.get() == 0 ) {
									valueIndexCounter.set( entityDescriptor.getIdentifierMapping().getJdbcTypeCount() );
								}
								valueIndex = valueIndexCounter.get();
								valueIndexCounter.plus( fkTargetPart.getJdbcTypeCount() );
							}

							executeDelete(
									pluralAttribute.getSeparateCollectionTable(),
									entityDescriptor,
									() -> fkTargetPart::forEachSelectable,
									idsAndFks,
									valueIndex,
									fkTargetPart,
									jdbcParameterBindings,
									executionContext
							);
						}
					}
				}
		);

		entityDescriptor.visitConstraintOrderedTables(
				(tableExpression, tableKeyColumnsVisitationSupplier) -> {
					executeDelete(
							tableExpression,
							entityDescriptor,
							tableKeyColumnsVisitationSupplier,
							idsAndFks,
							0,
							null,
							jdbcParameterBindings,
							executionContext
					);
				}
		);

		return idsAndFks.size();
	}

	private void executeDelete(
			String targetTableExpression,
			EntityMappingType entityDescriptor,
			Supplier<Consumer<SelectableConsumer>> tableKeyColumnsVisitationSupplier,
			List<Object> ids,
			int valueIndex,
			ModelPart valueModelPart,
			JdbcParameterBindings jdbcParameterBindings,
			DomainQueryExecutionContext executionContext) {
		final NamedTableReference targetTableReference = new NamedTableReference(
				targetTableExpression,
				DeleteStatement.DEFAULT_ALIAS,
				false,
				sessionFactory
		);

		final SqmJdbcExecutionContextAdapter executionContextAdapter = SqmJdbcExecutionContextAdapter.omittingLockingAndPaging( executionContext );

		final Predicate matchingIdsPredicate = matchingIdsPredicateProducer.produceRestriction(
				ids,
				entityDescriptor,
				valueIndex,
				valueModelPart,
				targetTableReference,
				tableKeyColumnsVisitationSupplier,
				executionContextAdapter
		);

		final DeleteStatement deleteStatement = new DeleteStatement( targetTableReference, matchingIdsPredicate );

		final JdbcDelete jdbcOperation = sqlAstTranslatorFactory.buildDeleteTranslator( sessionFactory, deleteStatement )
				.translate( jdbcParameterBindings, executionContext.getQueryOptions() );

		jdbcMutationExecutor.execute(
				jdbcOperation,
				jdbcParameterBindings,
				this::prepareQueryStatement,
				(integer, preparedStatement) -> {},
				executionContextAdapter
		);
	}

	private PreparedStatement prepareQueryStatement(String sql) {
		return StatementCreatorHelper.prepareQueryStatement( sql, executionContext.getSession() );
	}
}
