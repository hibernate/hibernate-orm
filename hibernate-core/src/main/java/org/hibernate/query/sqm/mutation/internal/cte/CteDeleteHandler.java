/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.cte;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.internal.DeleteHandler;
import org.hibernate.query.sqm.mutation.internal.MultiTableSqmMutationConverter;
import org.hibernate.query.sqm.sql.internal.SqlAstQueryPartProcessingStateImpl;
import org.hibernate.query.sqm.tree.cte.SqmCteTable;
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
import org.hibernate.sql.results.graph.basic.BasicResult;

/**
 * Bulk-id delete handler that uses CTE and VALUES lists.
 *
 * @author Christian Beikov
 */
@SuppressWarnings("WeakerAccess")
public class CteDeleteHandler extends AbstractCteMutationHandler implements DeleteHandler {

	protected CteDeleteHandler(
			SqmCteTable cteTable,
			SqmDeleteStatement<?> sqmDeleteStatement,
			DomainParameterXref domainParameterXref,
			CteMutationStrategy strategy,
			SessionFactoryImplementor sessionFactory) {
		super( cteTable, sqmDeleteStatement, domainParameterXref, strategy, sessionFactory );
	}

	@Override
	protected void addDmlCtes(
			CteContainer statement,
			CteStatement idSelectCte,
			MultiTableSqmMutationConverter sqmConverter,
			Map<SqmParameter, List<JdbcParameter>> parameterResolutions,
			SessionFactoryImplementor factory) {
		final TableGroup updatingTableGroup = sqmConverter.getMutatingTableGroup();
		final SelectStatement idSelectStatement = (SelectStatement) idSelectCte.getCteDefinition();
		sqmConverter.getProcessingStateStack().push(
				new SqlAstQueryPartProcessingStateImpl(
						idSelectStatement.getQuerySpec(),
						sqmConverter.getCurrentProcessingState(),
						sqmConverter.getSqlAstCreationState(),
						sqmConverter.getCurrentClauseStack()::getCurrent
				)
		);
		getEntityDescriptor().visitSubTypeAttributeMappings(
				attribute -> {
					if ( attribute instanceof PluralAttributeMapping ) {
						final PluralAttributeMapping pluralAttribute = (PluralAttributeMapping) attribute;

						if ( pluralAttribute.getSeparateCollectionTable() != null ) {
							// Ensure that the FK target columns are available
							final boolean useFkTarget = !( pluralAttribute.getKeyDescriptor()
									.getTargetPart() instanceof EntityIdentifierMapping );
							if ( useFkTarget ) {
								final TableGroup mutatingTableGroup = sqmConverter.getMutatingTableGroup();
								pluralAttribute.getKeyDescriptor().getTargetPart().applySqlSelections(
										mutatingTableGroup.getNavigablePath(),
										mutatingTableGroup,
										sqmConverter,
										(selection, jdbcMapping) -> {
											idSelectStatement.getDomainResultDescriptors().add(
													new BasicResult<>(
															selection.getValuesArrayPosition(),
															null,
															jdbcMapping.getJavaTypeDescriptor()
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
									idSelectCte.getCteTable().getCteColumns(),
									factory
							);
							final NamedTableReference dmlTableReference = new NamedTableReference(
									tableExpression,
									DeleteStatement.DEFAULT_ALIAS,
									true,
									factory
							);
							final List<ColumnReference> columnReferences = new ArrayList<>( idSelectCte.getCteTable().getCteColumns().size() );
							pluralAttribute.getKeyDescriptor().visitKeySelectables(
									(index, selectable) -> columnReferences.add(
											new ColumnReference(
													dmlTableReference,
													selectable,
													factory
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
				}
		);
		sqmConverter.getProcessingStateStack().pop();

		getEntityDescriptor().visitConstraintOrderedTables(
				(tableExpression, tableColumnsVisitationSupplier) -> {
					final CteTable dmlResultCte = new CteTable(
							getCteTableName( tableExpression ),
							idSelectCte.getCteTable().getCteColumns(),
							factory
					);
					final TableReference updatingTableReference = updatingTableGroup.getTableReference(
							updatingTableGroup.getNavigablePath(),
							tableExpression,
							true,
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
											selectable,
											factory
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

	protected String getCteTableName(PluralAttributeMapping pluralAttribute) {
		final String hibernateEntityName = pluralAttribute.findContainingEntityMapping().getEntityName();
		final String jpaEntityName = getSessionFactory().getJpaMetamodel().entity( hibernateEntityName ).getName();
		return DML_RESULT_TABLE_NAME_PREFIX + jpaEntityName + "_" + pluralAttribute.getRootPathName().substring(
				hibernateEntityName.length() + 1
		);
	}
}
