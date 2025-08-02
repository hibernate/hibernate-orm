/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal.cte;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.internal.DeleteHandler;
import org.hibernate.query.sqm.mutation.internal.MultiTableSqmMutationConverter;
import org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper;
import org.hibernate.query.sqm.sql.internal.SqlAstQueryPartProcessingStateImpl;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.cte.CteTable;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.graph.basic.BasicResult;

/**
 * Bulk mutation delete handler that uses CTE and VALUES lists.
 *
 * @author Christian Beikov
 */
public class CteDeleteHandler extends AbstractCteMutationHandler implements DeleteHandler {
	private static final String DELETE_RESULT_TABLE_NAME_PREFIX = "delete_cte_";

	protected CteDeleteHandler(
			CteTable cteTable,
			SqmDeleteStatement<?> sqmDeleteStatement,
			DomainParameterXref domainParameterXref,
			CteMutationStrategy strategy,
			SessionFactoryImplementor sessionFactory,
			DomainQueryExecutionContext context,
			MutableObject<JdbcParameterBindings> firstJdbcParameterBindingsConsumer) {
		super( cteTable, sqmDeleteStatement, domainParameterXref, strategy, sessionFactory, context, firstJdbcParameterBindingsConsumer );
	}

	@Override
	protected void addDmlCtes(
			CteContainer statement,
			CteStatement idSelectCte,
			MultiTableSqmMutationConverter sqmConverter,
			Map<SqmParameter<?>, List<JdbcParameter>> parameterResolutions,
			SessionFactoryImplementor factory) {
		final TableGroup mutatingTableGroup = sqmConverter.getMutatingTableGroup();
		final SelectStatement idSelectStatement = (SelectStatement) idSelectCte.getCteDefinition();
		sqmConverter.getProcessingStateStack().push(
				new SqlAstQueryPartProcessingStateImpl(
						idSelectStatement.getQuerySpec(),
						sqmConverter.getCurrentProcessingState(),
						sqmConverter.getSqlAstCreationState(),
						sqmConverter.getCurrentClauseStack()::getCurrent,
						false
				)
		);
		SqmMutationStrategyHelper.visitCollectionTables(
				(EntityMappingType) mutatingTableGroup.getModelPart(),
				pluralAttribute -> {
					if ( pluralAttribute.getSeparateCollectionTable() != null ) {
						// Ensure that the FK target columns are available
						final boolean useFkTarget = !pluralAttribute.getKeyDescriptor()
								.getTargetPart().isEntityIdentifierMapping();
						if ( useFkTarget ) {
							pluralAttribute.getKeyDescriptor().getTargetPart().applySqlSelections(
									mutatingTableGroup.getNavigablePath(),
									mutatingTableGroup,
									sqmConverter,
									(selection, jdbcMapping) -> {
										idSelectStatement.getDomainResultDescriptors().add(
												new BasicResult<>(
														selection.getValuesArrayPosition(),
														null,
														jdbcMapping
												)
										);
									}
							);
						}

						// this collection has a separate collection table, meaning it is one of:
						//		1) element-collection
						//		2) many-to-many
						//		3) one-to many using a dedicated join-table
						//
						// in all of these cases, we should clean up the matching rows in the
						// collection table
						final String tableExpression = pluralAttribute.getSeparateCollectionTable();
						final CteTable dmlResultCte = new CteTable(
								getCteTableName( pluralAttribute ),
								idSelectCte.getCteTable().getCteColumns()

						);
						final NamedTableReference dmlTableReference = new NamedTableReference(
								tableExpression,
								DeleteStatement.DEFAULT_ALIAS,
								true
							);
							final List<ColumnReference> columnReferences = new ArrayList<>( idSelectCte.getCteTable().getCteColumns().size() );
							pluralAttribute.getKeyDescriptor().visitKeySelectables(
									(index, selectable) -> columnReferences.add(
											new ColumnReference(
													dmlTableReference,
													selectable
										)
								)
						);
						final MutationStatement dmlStatement = new DeleteStatement(
								dmlTableReference,
								createIdSubQueryPredicate(
										columnReferences,
										idSelectCte,
										useFkTarget ? pluralAttribute.getKeyDescriptor().getTargetPart() : null,
										factory
								),
								columnReferences
						);
						statement.addCteStatement( new CteStatement( dmlResultCte, dmlStatement ) );
					}
				}
		);

		sqmConverter.getProcessingStateStack().pop();

		applyDmlOperations( statement, idSelectCte, factory, mutatingTableGroup );
	}

	protected void applyDmlOperations(
			CteContainer statement,
			CteStatement idSelectCte,
			SessionFactoryImplementor factory,
			TableGroup updatingTableGroup) {
		getEntityDescriptor().visitConstraintOrderedTables(
				(tableExpression, tableColumnsVisitationSupplier) -> {
					final String cteTableName = getCteTableName( tableExpression );
					if ( statement.getCteStatement( cteTableName ) != null ) {
						// Since secondary tables could appear multiple times, we have to skip duplicates
						return;
					}
					final CteTable dmlResultCte = new CteTable(
							cteTableName,
							idSelectCte.getCteTable().getCteColumns()
					);
					final TableReference updatingTableReference = updatingTableGroup.getTableReference(
							updatingTableGroup.getNavigablePath(),
							tableExpression,
							true
					);
					final NamedTableReference dmlTableReference = resolveUnionTableReference(
							updatingTableReference,
							tableExpression
					);
					final List<ColumnReference> columnReferences = new ArrayList<>( idSelectCte.getCteTable().getCteColumns().size() );
					tableColumnsVisitationSupplier.get().accept(
							(index, selectable) -> columnReferences.add(
									new ColumnReference(
											dmlTableReference,
											selectable
									)
							)
					);
					final MutationStatement dmlStatement = new DeleteStatement(
							dmlTableReference,
							createIdSubQueryPredicate( columnReferences, idSelectCte, factory ),
							columnReferences
					);
					statement.addCteStatement( new CteStatement( dmlResultCte, dmlStatement ) );
				}
		);
	}

	@Override
	protected String getCteTableName(String tableExpression) {
		final Dialect dialect = getSessionFactory().getJdbcServices().getDialect();
		if ( Identifier.isQuoted( tableExpression ) ) {
			tableExpression = QualifiedNameParser.INSTANCE.parse( tableExpression ).getObjectName().getText();
		}
		return Identifier.toIdentifier( DELETE_RESULT_TABLE_NAME_PREFIX + tableExpression ).render( dialect );
	}

	protected String getCteTableName(PluralAttributeMapping pluralAttribute) {
		final Dialect dialect = getSessionFactory().getJdbcServices().getDialect();
		final String hibernateEntityName = pluralAttribute.findContainingEntityMapping().getEntityName();
		final String jpaEntityName = getSessionFactory().getJpaMetamodel().entity( hibernateEntityName ).getName();
		return Identifier.toIdentifier(
				DELETE_RESULT_TABLE_NAME_PREFIX + jpaEntityName + "_" +
						pluralAttribute.getRootPathName().substring( hibernateEntityName.length() + 1 )
		).render( dialect );
	}
}
