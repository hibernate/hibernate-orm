/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.AssociationKey;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.SelectionConsumer;
import org.hibernate.metamodel.mapping.SelectionMappings;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
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

/**
 * @author Andrea Boriero
 */
public class EmbeddedForeignKeyDescriptor implements ForeignKeyDescriptor, ModelPart {

	private final EmbeddableValuedModelPart mappingType;
	private final String keyColumnContainingTable;
	private final SelectionMappings keySelectionMappings;
	private final String targetColumnContainingTable;
	private final SelectionMappings targetSelectionMappings;
	private AssociationKey associationKey;

	public EmbeddedForeignKeyDescriptor(
			EmbeddableValuedModelPart mappingType,
			String keyColumnContainingTable,
			SelectionMappings keySelectionMappings,
			String targetColumnContainingTable,
			SelectionMappings targetSelectionMappings,
			MappingModelCreationProcess creationProcess) {
		this.keyColumnContainingTable = keyColumnContainingTable;
		this.keySelectionMappings = keySelectionMappings;
		this.targetColumnContainingTable = targetColumnContainingTable;
		this.targetSelectionMappings = targetSelectionMappings;
		this.mappingType = mappingType;

		creationProcess.registerInitializationCallback(
				"Embedded (composite) FK descriptor " + mappingType.getNavigableRole(),
				() -> {
					// todo (6.0) : how to make sure things we need are ready to go?
					// 		- e.g., here, we need access to the sub-attributes
					final List<AttributeMapping> subAttributes = mappingType.getEmbeddableTypeDescriptor().getAttributeMappings();
					if ( subAttributes.isEmpty() ) {
						// todo (6.0) : ^^ for now, this is the only way we "know" that the embeddable has not been finalized yet
						return false;
					}

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
			return createDomainResult(
					collectionPath,
					tableGroup,
					targetColumnContainingTable,
					targetSelectionMappings,
					creationState
			);
		}
		else {
			return createDomainResult(
					collectionPath,
					tableGroup,
					keyColumnContainingTable,
					keySelectionMappings,
					creationState
			);
		}
	}

	@Override
	public DomainResult createDomainResult(
			NavigablePath collectionPath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		return createDomainResult(
				collectionPath,
				tableGroup,
				keyColumnContainingTable,
				keySelectionMappings,
				creationState
		);
	}

	@Override
	public DomainResult createDomainResult(
			NavigablePath collectionPath,
			TableGroup tableGroup,
			boolean isKeyReferringSide,
			DomainResultCreationState creationState) {
		if ( isKeyReferringSide ) {
			return createDomainResult(
					collectionPath,
					tableGroup,
					keyColumnContainingTable,
					keySelectionMappings,
					creationState
			);
		}
		else {
			return createDomainResult(
					collectionPath,
					tableGroup,
					targetColumnContainingTable,
					targetSelectionMappings,
					creationState
			);
		}
	}

	private DomainResult createDomainResult(
			NavigablePath collectionPath,
			TableGroup tableGroup,
			String columnContainingTable,
			SelectionMappings selectionMappings,
			DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final TableReference tableReference = tableGroup.resolveTableReference( columnContainingTable );
		final String identificationVariable = tableReference.getIdentificationVariable();

		final List<SqlSelection> sqlSelections = new ArrayList<>( selectionMappings.getJdbcTypeCount() );
		selectionMappings.forEachSelection(
				(columnIndex, selection) -> {
					final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
							sqlExpressionResolver.resolveSqlExpression(
									SqlExpressionResolver.createColumnReferenceKey(
											tableReference,
											selection.getSelectionExpression()
									),
									s ->
											new ColumnReference(
													identificationVariable,
													selection,
													creationState.getSqlAstCreationState()
															.getCreationContext()
															.getSessionFactory()
											)
							),
							selection.getJdbcMapping().getJavaTypeDescriptor(),
							sqlAstCreationState.getCreationContext().getDomainModel().getTypeConfiguration()
					);
					sqlSelections.add( sqlSelection );
				}
		);

		return new EmbeddableForeignKeyResultImpl<>(
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
			return getPredicate( lhs, rhs, creationContext, keySelectionMappings, targetSelectionMappings );
		}
		else {
			assert rhsTableExpression.equals( keyColumnContainingTable );
			return getPredicate( lhs, rhs, creationContext, targetSelectionMappings, keySelectionMappings );
		}
	}

	private Predicate getPredicate(
			TableReference lhs,
			TableReference rhs,
			SqlAstCreationContext creationContext,
			SelectionMappings lhsMappings,
			SelectionMappings rhsMappings) {
		final Junction predicate = new Junction( Junction.Nature.CONJUNCTION );
		lhsMappings.forEachSelection(
				(i, selection) -> {
					final ComparisonPredicate comparisonPredicate = new ComparisonPredicate(
							new ColumnReference(
									lhs,
									selection,
									creationContext.getSessionFactory()
							),
							ComparisonOperator.EQUAL,
							new ColumnReference(
									rhs,
									rhsMappings.getSelectionMapping( i ),
									creationContext.getSessionFactory()
							)
					);
					predicate.add( comparisonPredicate );
				}
		);
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
	public int visitReferringColumns(int offset, SelectionConsumer consumer) {
		return keySelectionMappings.forEachSelection( offset, consumer );
	}

	@Override
	public int visitTargetColumns(int offset, SelectionConsumer consumer) {
		return targetSelectionMappings.forEachSelection( offset, consumer );
	}

	@Override
	public AssociationKey getAssociationKey() {
		if ( associationKey == null ) {
			final List<String> columns = new ArrayList<>();
			keySelectionMappings.forEachSelection(
					(columnIndex, selection) -> {
						columns.add( selection.getSelectionExpression() );
					}
			);
			associationKey = new AssociationKey( keyColumnContainingTable, columns );
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
		final int size = keySelectionMappings.getJdbcTypeCount();
		final List<SqlSelection> sqlSelections = new ArrayList<>( size );
		keySelectionMappings.forEachSelection(
				(columnIndex, selection) -> {
					final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
							sqlExpressionResolver.resolveSqlExpression(
									SqlExpressionResolver.createColumnReferenceKey(
											tableReference,
											selection.getSelectionExpression()
									),
									s ->
											new ColumnReference(
													identificationVariable,
													selection,
													creationState.getSqlAstCreationState()
															.getCreationContext()
															.getSessionFactory()
											)
							),
							selection.getJdbcMapping().getJavaTypeDescriptor(),
							sqlAstCreationState.getCreationContext().getDomainModel().getTypeConfiguration()
					);
					sqlSelections.add( sqlSelection );
				}
		);

		return new EmbeddableForeignKeyResultImpl<>(
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
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		return mappingType.forEachJdbcType( offset, action );
	}

	@Override
	public int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		return mappingType.forEachDisassembledJdbcValue( value, clause, offset, valuesConsumer, session );
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return mappingType.disassemble( value, session );
	}
}
