/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.Consumer;

import org.hibernate.annotations.NotFoundAction;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Map;
import org.hibernate.metamodel.mapping.AssociationKey;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.OneToManyTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.predicate.Predicate;

import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Objects.requireNonNullElse;

/**
 * order( id, ... )
 * item( id, order_fk, ... )
 *
 * `Order#items`
 * 		table : item
 * 		key : order_fk
 * 		element : id
 *
 * @author Steve Ebersole
 */
public class OneToManyCollectionPart extends AbstractEntityCollectionPart implements TableGroupJoinProducer {
	private final String mapKeyPropertyName;
	private AssociationKey fetchAssociationKey;

	public OneToManyCollectionPart(
			Nature nature,
			Collection bootCollectionDescriptor,
			CollectionPersister collectionDescriptor,
			EntityMappingType elementTypeDescriptor,
			MappingModelCreationProcess creationProcess) {
		this( nature, bootCollectionDescriptor, collectionDescriptor, elementTypeDescriptor, NotFoundAction.EXCEPTION, creationProcess );
	}

	public OneToManyCollectionPart(
			Nature nature,
			Collection bootCollectionDescriptor,
			CollectionPersister collectionDescriptor,
			EntityMappingType elementTypeDescriptor,
			NotFoundAction notFoundAction,
			MappingModelCreationProcess creationProcess) {
		super( nature, bootCollectionDescriptor, collectionDescriptor, elementTypeDescriptor, notFoundAction, creationProcess );

		if ( nature == Nature.INDEX && bootCollectionDescriptor instanceof Map ) {
			mapKeyPropertyName = ( (Map) bootCollectionDescriptor ).getMapKeyPropertyName();
		}
		else {
			mapKeyPropertyName = null;
		}
	}

	/**
	 * For Hibernate Reactive
	 */
	protected OneToManyCollectionPart(OneToManyCollectionPart original) {
		super( original );
		this.mapKeyPropertyName = original.mapKeyPropertyName;
		this.fetchAssociationKey = original.fetchAssociationKey;
	}

	@Override
	public Cardinality getCardinality() {
		return Cardinality.ONE_TO_MANY;
	}

	@Override
	public <X, Y> int breakDownJdbcValues(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		return getAssociatedEntityMappingType().getIdentifierMapping().breakDownJdbcValues(
				disassemble( domainValue, session ),
				offset,
				x,
				y,
				valueConsumer,
				session
		);
	}

	@Override
	public String getContainingTableExpression() {
		return getCollectionDescriptor().getAttributeMapping().getKeyDescriptor().getContainingTableExpression();
	}

	@Override
	public SelectableMapping getSelectable(int columnIndex) {
		return getCollectionDescriptor().getAttributeMapping().getKeyDescriptor().getSelectable( columnIndex );
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		return getCollectionDescriptor().getAttributeMapping().getKeyDescriptor().getKeyPart().forEachSelectable( offset, consumer );
	}

	@Override
	protected AssociationKey resolveFetchAssociationKey() {
		return fetchAssociationKey;
	}

	@Override
	public boolean hasPartitionedSelectionMapping() {
		return false;
	}

	@Override
	public boolean containsTableReference(String tableExpression) {
		return getAssociatedEntityMappingType().containsTableReference( tableExpression );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// TableGroupJoinProducer

	@Override
	public SqlAstJoinType getDefaultSqlAstJoinType(TableGroup parentTableGroup) {
		return SqlAstJoinType.INNER;
	}

	@Override
	public boolean isSimpleJoinPredicate(Predicate predicate) {
		return getCollectionDescriptor().getAttributeMapping().getKeyDescriptor().isSimpleJoinPredicate( predicate );
	}

	@Override
	public TableGroupJoin createTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			@Nullable String explicitSourceAlias,
			@Nullable SqlAliasBase explicitSqlAliasBase,
			@Nullable SqlAstJoinType requestedJoinType,
			boolean fetched,
			boolean addsPredicate,
			SqlAstCreationState creationState) {
		final SqlAstJoinType joinType = requireNonNullElse( requestedJoinType, SqlAstJoinType.INNER );
		final TableGroup elementTableGroup = ( (OneToManyTableGroup) lhs ).getElementTableGroup();

		// INDEX is implied if mapKeyPropertyName is not null
		if ( mapKeyPropertyName != null ) {
			final EntityCollectionPart elementPart = (EntityCollectionPart) getCollectionDescriptor().getAttributeMapping().getElementDescriptor();
			final EntityMappingType elementEntity = elementPart.getAssociatedEntityMappingType();
			final AttributeMapping mapKeyAttribute = elementEntity.findAttributeMapping( mapKeyPropertyName );
			if ( mapKeyAttribute instanceof ToOneAttributeMapping toOne ) {
				final NavigablePath mapKeyPropertyPath = navigablePath.append( mapKeyPropertyName );
				final TableGroupJoin tableGroupJoin = toOne.createTableGroupJoin(
						mapKeyPropertyPath,
						elementTableGroup,
						null,
						null,
						null,
						fetched,
						addsPredicate,
						creationState
				);
				creationState.getFromClauseAccess().registerTableGroup( mapKeyPropertyPath, tableGroupJoin.getJoinedGroup() );
				return tableGroupJoin;
			}
		}

		return new TableGroupJoin( navigablePath, joinType, elementTableGroup, null );
	}

	@Override
	public TableGroup createRootTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			@Nullable String explicitSourceAlias,
			@Nullable SqlAliasBase explicitSqlAliasBase,
			@Nullable SqlAstJoinType sqlAstJoinType,
			boolean fetched,
			@Nullable Consumer<Predicate> predicateConsumer,
			SqlAstCreationState creationState) {
		return createTableGroupInternal(
				true,
				navigablePath,
				fetched,
				explicitSourceAlias,
				creationState.getSqlAliasBaseGenerator().createSqlAliasBase( getSqlAliasStem() ),
				creationState
		);
	}

	public TableGroup createAssociatedTableGroup(
			boolean canUseInnerJoins,
			NavigablePath append,
			boolean fetched,
			String sourceAlias,
			SqlAliasBase sqlAliasBase,
			SqlAstCreationState creationState) {
		return createTableGroupInternal(
				canUseInnerJoins,
				append,
				fetched,
				sourceAlias,
				sqlAliasBase,
				creationState
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Initialization

	@Override
	public boolean finishInitialization(
			CollectionPersister collectionDescriptor,
			Collection bootValueMapping,
			String fkTargetModelPartName,
			MappingModelCreationProcess creationProcess) {
		final PluralAttributeMapping pluralAttribute = getCollectionDescriptor().getAttributeMapping();
		if ( pluralAttribute == null ) {
			return false;
		}

		final ForeignKeyDescriptor foreignKey = pluralAttribute.getKeyDescriptor();
		if ( foreignKey == null ) {
			return false;
		}

		fetchAssociationKey = foreignKey.getAssociationKey();
		return true;
	}

	@Override
	public JdbcMapping getJdbcMapping(final int index) {
		return getEntityMappingType().getJdbcMapping( index );
	}

}
