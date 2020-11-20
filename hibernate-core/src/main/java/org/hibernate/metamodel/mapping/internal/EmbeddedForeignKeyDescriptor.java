/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.internal.AbstractCompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.AssociationKey;
import org.hibernate.metamodel.mapping.ColumnConsumer;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableForeignKeyResultImpl;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Andrea Boriero
 */
public class EmbeddedForeignKeyDescriptor implements ForeignKeyDescriptor, ModelPart {

	private final EmbeddableValuedModelPart mappingType;
	private final String keyColumnContainingTable;
	private final List<String> keyColumnExpressions;
	private final String targetColumnContainingTable;
	private final List<String> targetColumnExpressions;
	private final List<JdbcMapping> jdbcMappings;
	private AssociationKey associationKey;

	public EmbeddedForeignKeyDescriptor(
			AbstractCompositeIdentifierMapping mappingType,
			String keyColumnContainingTable,
			List<String> keyColumnExpressions,
			String targetColumnContainingTable,
			List<String> targetColumnExpressions,
			MappingModelCreationProcess creationProcess) {
		this.keyColumnContainingTable = keyColumnContainingTable;
		this.keyColumnExpressions = keyColumnExpressions;
		this.targetColumnContainingTable = targetColumnContainingTable;
		this.targetColumnExpressions = targetColumnExpressions;
		this.mappingType = mappingType;
		jdbcMappings = new ArrayList<>();

		creationProcess.registerInitializationCallback(
				"Embedded (composite) FK descriptor " + mappingType.getNavigableRole(),
				() -> {
					// todo (6.0) : how to make sure things we need are ready to go?
					// 		- e.g., here, we need access to the sub-attributes
					final Collection<SingularAttributeMapping> subAttributes = mappingType.getAttributes();
					if ( subAttributes.isEmpty() ) {
						// todo (6.0) : ^^ for now, this is the only way we "know" that the embeddable has not been finalized yet
						return false;
					}

					subAttributes.forEach(
							attribute -> {
								final TypeConfiguration typeConfiguration = creationProcess
										.getCreationContext()
										.getTypeConfiguration();
								if ( attribute instanceof ToOneAttributeMapping ) {
									final ToOneAttributeMapping associationAttributeMapping = (ToOneAttributeMapping) attribute;
									associationAttributeMapping.getAssociatedEntityMappingType()
											.getEntityPersister()
											.getIdentifierMapping()
											.visitJdbcTypes( jdbcMappings::add, null, typeConfiguration );
								}
								else {
									attribute.visitJdbcTypes( jdbcMappings::add, null, typeConfiguration );
								}
							}
					);

					return true;
				}
		);
	}

	@Override
	public DomainResult createCollectionFetchDomainResult(
			NavigablePath collectionPath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		if ( targetColumnContainingTable.equals( keyColumnContainingTable ) ) {
			final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
			final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
			final TableReference tableReference = tableGroup.resolveTableReference( keyColumnContainingTable );
			final String identificationVariable = tableReference.getIdentificationVariable();

			List<SqlSelection> sqlSelections = new ArrayList<>();
			for ( int i = 0; i < keyColumnExpressions.size(); i++ ) {
				final JdbcMapping jdbcMapping = jdbcMappings.get( i );
				final String columnExpression = targetColumnExpressions.get( i );
				final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
						sqlExpressionResolver.resolveSqlExpression(
								SqlExpressionResolver.createColumnReferenceKey(
										tableReference,
										columnExpression
								),
								s ->
										new ColumnReference(
												identificationVariable,
												columnExpression,
												false,
												null,
												null,
												jdbcMapping,
												creationState.getSqlAstCreationState()
														.getCreationContext()
														.getSessionFactory()
										)

						),
						jdbcMapping.getJavaTypeDescriptor(),
						sqlAstCreationState.getCreationContext().getDomainModel().getTypeConfiguration()
				);
				sqlSelections.add( sqlSelection );
			}

			return new EmbeddableForeignKeyResultImpl(
					sqlSelections,
					collectionPath,
					mappingType,
					null,
					creationState
			);
		}
		else {
			return createDomainResult( collectionPath, tableGroup, creationState );
		}
	}

	@Override
	public DomainResult createDomainResult(
			NavigablePath collectionPath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		//noinspection unchecked
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final TableReference tableReference = tableGroup.resolveTableReference( keyColumnContainingTable );
		final String identificationVariable = tableReference.getIdentificationVariable();
		int size = keyColumnExpressions.size();
		List<SqlSelection> sqlSelections = new ArrayList<>(size);
		for ( int i = 0; i < size; i++ ) {
			final String columnExpression = keyColumnExpressions.get( i );
			final JdbcMapping jdbcMapping = jdbcMappings.get( i );
			final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
					sqlExpressionResolver.resolveSqlExpression(
							SqlExpressionResolver.createColumnReferenceKey(
									tableReference,
									columnExpression
							),
							s ->
									new ColumnReference(
											identificationVariable,
											columnExpression,
											false,
											null,
											null,
											jdbcMapping,
											creationState.getSqlAstCreationState()
													.getCreationContext()
													.getSessionFactory()
									)
					),
					jdbcMapping.getJavaTypeDescriptor(),
					sqlAstCreationState.getCreationContext().getDomainModel().getTypeConfiguration()
			);
			sqlSelections.add( sqlSelection );
		}

		return new EmbeddableForeignKeyResultImpl(
				sqlSelections,
				collectionPath,
				mappingType,
				null,
				creationState
		);
	}

	@Override
	public Predicate generateJoinPredicate(
			TableGroup lhs,
			TableGroup tableGroup,
			SqlAstJoinType sqlAstJoinType,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		TableReference lhsTableReference;
		TableReference rhsTableKeyReference;
		if ( targetColumnContainingTable.equals( keyColumnContainingTable ) ) {
			lhsTableReference = getTableReferenceWhenTargetEqualsKey( lhs, tableGroup, keyColumnContainingTable );

			rhsTableKeyReference = getTableReference(
					lhs,
					tableGroup,
					targetColumnContainingTable
			);
		}
		else {
			lhsTableReference = getTableReference( lhs, tableGroup, keyColumnContainingTable );

			rhsTableKeyReference = getTableReference(
					lhs,
					tableGroup,
					targetColumnContainingTable
			);
		}

		return generateJoinPredicate(
				lhsTableReference,
				rhsTableKeyReference,
				sqlAstJoinType,
				sqlExpressionResolver,
				creationContext
		);
	}

	@Override
	public Predicate generateJoinPredicate(
			TableReference lhs,
			TableReference rhs,
			SqlAstJoinType sqlAstJoinType,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		final String rhsTableExpression = rhs.getTableExpression();
		final String lhsTableExpression = lhs.getTableExpression();
		if ( lhsTableExpression.equals( keyColumnContainingTable ) ) {
			assert rhsTableExpression.equals( targetColumnContainingTable );
			return getPredicate( lhs, rhs, creationContext, keyColumnExpressions, targetColumnExpressions );
		}
		else {
			assert rhsTableExpression.equals( keyColumnContainingTable );
			return getPredicate( lhs, rhs, creationContext, targetColumnExpressions, keyColumnExpressions );
		}
	}

	private Predicate getPredicate(
			TableReference lhs,
			TableReference rhs,
			SqlAstCreationContext creationContext,
			List<String> lhsExpressions,
			List<String> rhsColumnExpressions) {
		final Junction predicate = new Junction( Junction.Nature.CONJUNCTION );
		for ( int i = 0; i < lhsExpressions.size(); i++ ) {
			final JdbcMapping jdbcMapping = jdbcMappings.get( i );
			final ComparisonPredicate comparisonPredicate = new ComparisonPredicate(
					new ColumnReference(
							lhs,
							lhsExpressions.get( i ),
							false,
							null,
							null,
							jdbcMapping,
							creationContext.getSessionFactory()
					),
					ComparisonOperator.EQUAL,
					new ColumnReference(
							rhs,
							rhsColumnExpressions.get( i ),
							false,
							null,
							null,
							jdbcMapping,
							creationContext.getSessionFactory()
					)
			);
			predicate.add( comparisonPredicate );
		}
		return predicate;
	}

	protected TableReference getTableReferenceWhenTargetEqualsKey(TableGroup lhs, TableGroup tableGroup, String table) {
		if ( tableGroup.getPrimaryTableReference().getTableExpression().equals( table ) ) {
			return tableGroup.getPrimaryTableReference();
		}
		if ( lhs.getPrimaryTableReference().getTableExpression().equals( table ) ) {
			return lhs.getPrimaryTableReference();
		}

		for ( TableReferenceJoin tableJoin : lhs.getTableReferenceJoins() ) {
			if ( tableJoin.getJoinedTableReference().getTableExpression().equals( table ) ) {
				return tableJoin.getJoinedTableReference();
			}
		}

		throw new IllegalStateException( "Could not resolve binding for table `" + table + "`" );
	}

	protected TableReference getTableReference(TableGroup lhs, TableGroup tableGroup, String table) {
		if ( lhs.getPrimaryTableReference().getTableExpression().equals( table ) ) {
			return lhs.getPrimaryTableReference();
		}
		else if ( tableGroup.getPrimaryTableReference().getTableExpression().equals( table ) ) {
			return tableGroup.getPrimaryTableReference();
		}

		final TableReference tableReference = lhs.resolveTableReference( table );
		if ( tableReference != null ) {
			return tableReference;
		}

		throw new IllegalStateException( "Could not resolve binding for table `" + table + "`" );
	}

	@Override
	public void visitReferringColumns(ColumnConsumer consumer) {
		for ( int i = 0; i < keyColumnExpressions.size(); i++ ) {
			consumer.accept(
					keyColumnContainingTable,
					keyColumnExpressions.get( i ),
					false,
					null,
					null,
					jdbcMappings.get( i )
			);
		}
	}

	@Override
	public void visitTargetColumns(ColumnConsumer consumer) {
		for ( int i = 0; i < keyColumnExpressions.size(); i++ ) {
			consumer.accept(
					targetColumnContainingTable,
					targetColumnExpressions.get( i ),
					false,
					null,
					null,
					jdbcMappings.get( i )
			);
		}
	}

	@Override
	public AssociationKey getAssociationKey() {
		if ( associationKey == null ) {
			associationKey = new AssociationKey( keyColumnContainingTable, keyColumnExpressions );
		}
		return associationKey;
	}

	@Override
	public MappingType getPartMappingType() {
		throw new HibernateException( "Unexpected call to SimpleForeignKeyDescriptor#getPartMappingType" );
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return mappingType.getJavaTypeDescriptor();
	}

	@Override
	public NavigableRole getNavigableRole() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		//noinspection unchecked
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final TableReference tableReference = tableGroup.resolveTableReference( keyColumnContainingTable );
		final String identificationVariable = tableReference.getIdentificationVariable();
		int size = keyColumnExpressions.size();
		List<SqlSelection> sqlSelections = new ArrayList<>(size);
		for ( int i = 0; i < size; i++ ) {
			final String columnExpression = keyColumnExpressions.get( i );
			final JdbcMapping jdbcMapping = jdbcMappings.get( i );
			final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
					sqlExpressionResolver.resolveSqlExpression(
							SqlExpressionResolver.createColumnReferenceKey(
									tableReference,
									columnExpression
							),
							s ->
									new ColumnReference(
											identificationVariable,
											columnExpression,
											false,
											null,
											null,
											jdbcMapping,
											creationState.getSqlAstCreationState()
													.getCreationContext()
													.getSessionFactory()
									)
					),
					jdbcMapping.getJavaTypeDescriptor(),
					sqlAstCreationState.getCreationContext().getDomainModel().getTypeConfiguration()
			);
			sqlSelections.add( sqlSelection );
		}

		return new EmbeddableForeignKeyResultImpl(
				sqlSelections,
				navigablePath,
				mappingType,
				resultVariable,
				creationState
		);
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitJdbcTypes(
			Consumer<JdbcMapping> action, Clause clause, TypeConfiguration typeConfiguration) {
		mappingType.visitJdbcTypes( action, clause, typeConfiguration );
	}
}
