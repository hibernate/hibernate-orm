/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.AssociationKey;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
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
public class EmbeddedForeignKeyDescriptor implements ForeignKeyDescriptor {

	private final EmbeddableValuedModelPart keyMappingType;
	private final EmbeddableValuedModelPart targetMappingType;
	private final String keyTable;
	private final SelectableMappings keySelectableMappings;
	private final String targetTable;
	private final SelectableMappings targetSelectableMappings;
	private AssociationKey associationKey;

	public EmbeddedForeignKeyDescriptor(
			EmbeddableValuedModelPart keyMappingType,
			EmbeddableValuedModelPart targetMappingType,
			String keyTable,
			SelectableMappings keySelectableMappings,
			String targetTable,
			SelectableMappings targetSelectableMappings,
			MappingModelCreationProcess creationProcess) {
		this.keyTable = keyTable;
		this.keySelectableMappings = keySelectableMappings;
		this.targetTable = targetTable;
		this.targetSelectableMappings = targetSelectableMappings;
		this.targetMappingType = targetMappingType;
		this.keyMappingType = keyMappingType;

		creationProcess.registerInitializationCallback(
				"Embedded (composite) FK descriptor " + targetMappingType.getNavigableRole(),
				() -> {
					// todo (6.0) : how to make sure things we need are ready to go?
					// 		- e.g., here, we need access to the sub-attributes
					final List<AttributeMapping> subAttributes = targetMappingType.getEmbeddableTypeDescriptor().getAttributeMappings();
					if ( subAttributes.isEmpty() ) {
						// todo (6.0) : ^^ for now, this is the only way we "know" that the embeddable has not been finalized yet
						return false;
					}
					return true;
				}
		);
	}

	private EmbeddedForeignKeyDescriptor(
			EmbeddedForeignKeyDescriptor original,
			String keyTable,
			SelectableMappings keySelectableMappings,
			MappingModelCreationProcess creationProcess) {
		this.keyTable = keyTable;
		this.keySelectableMappings = keySelectableMappings;
		this.targetTable = original.targetTable;
		this.targetSelectableMappings = original.targetSelectableMappings;
		this.targetMappingType = original.targetMappingType;
		this.keyMappingType = EmbeddedAttributeMapping.createInverseModelPart(
				targetMappingType,
				keySelectableMappings,
				creationProcess
		);
	}

	@Override
	public String getKeyTable() {
		return keyTable;
	}

	@Override
	public String getTargetTable() {
		return targetTable;
	}

	@Override
	public ForeignKeyDescriptor withKeySelectionMapping(
			IntFunction<SelectableMapping> selectableMappingAccess,
			MappingModelCreationProcess creationProcess) {
		SelectableMapping[] selectionMappings = new SelectableMapping[keySelectableMappings.getJdbcTypeCount()];
		for ( int i = 0; i < selectionMappings.length; i++ ) {
			selectionMappings[i] = selectableMappingAccess.apply( i );
		}
		return new EmbeddedForeignKeyDescriptor(
				this,
				selectionMappings[0].getContainingTableExpression(),
				new SelectableMappingsImpl( selectionMappings ),
				creationProcess
		);
	}

	@Override
	public DomainResult<?> createKeyDomainResult(
			NavigablePath collectionPath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		assert tableGroup.getTableReference( collectionPath, keyTable ) != null;

		return createDomainResult(
				collectionPath,
				tableGroup,
				keyTable,
				keyMappingType,
				creationState
		);
	}

	@Override
	public DomainResult<?> createTargetDomainResult(
			NavigablePath collectionPath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		assert tableGroup.getTableReference( collectionPath, targetTable ) != null;

		return createDomainResult(
				collectionPath,
				tableGroup,
				targetTable,
				targetMappingType,
				creationState
		);
	}

	@Override
	public DomainResult createCollectionFetchDomainResult(
			NavigablePath collectionPath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		if ( targetTable.equals( keyTable ) ) {
			return createDomainResult(
					collectionPath,
					tableGroup,
					targetTable,
					targetMappingType,
					creationState
			);
		}
		else {
			return createDomainResult(
					collectionPath,
					tableGroup,
					keyTable,
					keyMappingType,
					creationState
			);
		}
	}

	@Override
	public DomainResult createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		return createDomainResult(
				navigablePath,
				tableGroup,
				keyTable,
				keyMappingType,
				creationState
		);
	}

	@Override
	public DomainResult createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			boolean isKeyReferringSide,
			DomainResultCreationState creationState) {
		if ( isKeyReferringSide ) {
			return createDomainResult(
					navigablePath,
					tableGroup,
					keyTable,
					keyMappingType,
					creationState
			);
		}
		else {
			return createDomainResult(
					navigablePath,
					tableGroup,
					targetTable,
					targetMappingType,
					creationState
			);
		}
	}

	private DomainResult createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String columnContainingTable,
			EmbeddableValuedModelPart modelPart,
			DomainResultCreationState creationState) {
		final NavigablePath fkNavigablePath = navigablePath.append( getPartName() );
		final NavigablePath resultNavigablePath;
		if ( associationKey.getTable().equals( columnContainingTable ) ) {
			final ModelPartContainer parentModelPart = tableGroup.getModelPart();
			if ( parentModelPart instanceof PluralAttributeMapping ) {
				if ( ( (PluralAttributeMapping) parentModelPart ).getIndexDescriptor() == null ) {
					resultNavigablePath = navigablePath.append( CollectionPart.Nature.ELEMENT.getName() )
							.append( getPartName() );
				}
				else {
					resultNavigablePath = navigablePath.append( CollectionPart.Nature.INDEX.getName() )
							.append( getPartName() );
				}
			}
			else {
				resultNavigablePath = navigablePath.append( getPartName() );
			}
		}
		else {
			resultNavigablePath = navigablePath.append( getPartName() );
		}
		final TableGroup fkTableGroup = creationState.getSqlAstCreationState().getFromClauseAccess().resolveTableGroup(
				resultNavigablePath,
				np -> {
					final TableGroupJoin tableGroupJoin = modelPart.createTableGroupJoin(
							resultNavigablePath,
							tableGroup,
							null,
							SqlAstJoinType.INNER,
							true,
							LockMode.NONE,
							creationState.getSqlAstCreationState()
					);
					return tableGroupJoin.getJoinedGroup();
				}
		);
		if ( fkNavigablePath != resultNavigablePath ) {
			creationState.getSqlAstCreationState().getFromClauseAccess().resolveTableGroup(
					fkNavigablePath,
					np -> fkTableGroup
			);
		}

		return new EmbeddableForeignKeyResultImpl<>(
				resultNavigablePath,
				modelPart,
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
		if ( targetTable.equals( keyTable ) ) {
			lhsTableReference = getTableReferenceWhenTargetEqualsKey( lhs, tableGroup, keyTable );

			rhsTableKeyReference = getTableReference(
					lhs,
					tableGroup,
					targetTable
			);
		}
		else {
			lhsTableReference = getTableReference( lhs, tableGroup, keyTable );

			rhsTableKeyReference = getTableReference(
					lhs,
					tableGroup,
					targetTable
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
		if ( lhsTableExpression.equals( keyTable ) ) {
			assert rhsTableExpression.equals( targetTable );
			return getPredicate( lhs, rhs, creationContext, keySelectableMappings, targetSelectableMappings );
		}
		else {
			assert rhsTableExpression.equals( keyTable );
			return getPredicate( lhs, rhs, creationContext, targetSelectableMappings, keySelectableMappings );
		}
	}

	private Predicate getPredicate(
			TableReference lhs,
			TableReference rhs,
			SqlAstCreationContext creationContext,
			SelectableMappings lhsMappings,
			SelectableMappings rhsMappings) {
		final Junction predicate = new Junction( Junction.Nature.CONJUNCTION );
		lhsMappings.forEachSelectable(
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
									rhsMappings.getSelectable( i ),
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

		final TableReference tableReference = lhs.resolveTableReference(
				lhs.getNavigablePath()
						.append( getNavigableRole().getNavigableName() ),
				table
		);
		if ( tableReference != null ) {
			return tableReference;
		}

		throw new IllegalStateException( "Could not resolve binding for table `" + table + "`" );
	}

	@Override
	public int visitKeySelectables(int offset, SelectableConsumer consumer) {
		return keySelectableMappings.forEachSelectable( offset, consumer );
	}

	@Override
	public int visitTargetSelectables(int offset, SelectableConsumer consumer) {
		return targetSelectableMappings.forEachSelectable( offset, consumer );
	}

	@Override
	public AssociationKey getAssociationKey() {
		if ( associationKey == null ) {
			final List<String> columns = new ArrayList<>();
			keySelectableMappings.forEachSelectable(
					(columnIndex, selection) -> {
						columns.add( selection.getSelectionExpression() );
					}
			);
			associationKey = new AssociationKey( keyTable, columns );
		}
		return associationKey;
	}

	@Override
	public ModelPart getKeyPart() {
		return keyMappingType.getEmbeddableTypeDescriptor().getEmbeddedValueMapping();
	}

	@Override
	public ModelPart getTargetPart() {
		return targetMappingType.getEmbeddableTypeDescriptor().getEmbeddedValueMapping();
	}

	@Override
	public MappingType getPartMappingType() {
		return targetMappingType.getPartMappingType();
	}

	@Override
	public JavaTypeDescriptor<?> getJavaTypeDescriptor() {
		return targetMappingType.getJavaTypeDescriptor();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return targetMappingType.getNavigableRole();
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		final NavigablePath fkNavigablePath = navigablePath.append( getPartName() );
		creationState.getSqlAstCreationState().getFromClauseAccess().resolveTableGroup(
				fkNavigablePath,
				np -> {
					final TableGroupJoin tableGroupJoin = keyMappingType.createTableGroupJoin(
							fkNavigablePath,
							tableGroup,
							null,
							null,
							true,
							LockMode.NONE,
							creationState.getSqlAstCreationState()
					);
					return tableGroupJoin.getJoinedGroup();
				}

		);

		return new EmbeddableForeignKeyResultImpl<>(
				fkNavigablePath,
				keyMappingType,
				resultVariable,
				creationState
		);
	}

	@Override
	public void breakDownJdbcValues(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		assert domainValue instanceof Object[];

		final Object[] values = (Object[]) domainValue;

		keySelectableMappings.forEachSelectable(
				(index, selectable) -> valueConsumer.consume( values[ index ], selectable )
		);
	}

	@Override
	public Object getAssociationKeyFromTarget(Object targetObject, SharedSessionContractImplementor session) {
		// If the mapping type has an identifier type, that identifier is the key
		if ( targetMappingType instanceof SingleAttributeIdentifierMapping ) {
			return ( (SingleAttributeIdentifierMapping) targetMappingType ).getIdentifier( targetObject, session );
		}
		// Otherwise this is a key based on the target object i.e. without id-class
		return targetObject;
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return targetMappingType.findContainingEntityMapping();
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		return targetMappingType.forEachJdbcType( offset, action );
	}

	@Override
	public int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		return targetMappingType.forEachDisassembledJdbcValue( value, clause, offset, valuesConsumer, session );
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return targetMappingType.disassemble( value, session );
	}
}
