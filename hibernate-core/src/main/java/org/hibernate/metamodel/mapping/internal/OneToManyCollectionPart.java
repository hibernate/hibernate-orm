/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
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
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.from.OneToManyTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.predicate.Predicate;

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

	@Override
	public Cardinality getCardinality() {
		return Cardinality.ONE_TO_MANY;
	}

	@Override
	public void breakDownJdbcValues(
			Object domainValue,
			JdbcValueConsumer valueConsumer,
			SharedSessionContractImplementor session) {
		getAssociatedEntityMappingType().getIdentifierMapping().breakDownJdbcValues(
				disassemble( domainValue, session ),
				valueConsumer,
				session
		);
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}

		// should be an instance of the associated entity
		return getAssociatedEntityMappingType().getIdentifierMapping().getIdentifier( value );
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		return getCollectionDescriptor().getAttributeMapping().getKeyDescriptor().getKeyPart().forEachSelectable( offset, consumer );
//		return super.forEachSelectable( offset, consumer );
	}

	@Override
	protected AssociationKey resolveFetchAssociationKey() {
		return fetchAssociationKey;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// TableGroupJoinProducer
	//		todo (mutation) : this is only needed for `AbstractEntityCollectionPart#generateFetch`
	//			to create the map-key join

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
			TableGroup collectionTableGroup,
			String explicitSourceAlias,
			SqlAstJoinType requestedJoinType,
			boolean fetched,
			boolean addsPredicate,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			FromClauseAccess fromClauseAccess,
			SqlAstCreationContext creationContext) {
		final SqlAstJoinType joinType = requireNonNullElse( requestedJoinType, SqlAstJoinType.INNER );
		final TableGroup elementTableGroup = ( (OneToManyTableGroup) collectionTableGroup ).getElementTableGroup();

		// INDEX is implied if mapKeyPropertyName is not null
		if ( mapKeyPropertyName != null ) {
			final EntityCollectionPart elementPart = (EntityCollectionPart) getCollectionDescriptor().getAttributeMapping().getElementDescriptor();
			final EntityMappingType elementEntity = elementPart.getAssociatedEntityMappingType();
			final AttributeMapping mapKeyAttribute = elementEntity.findAttributeMapping( mapKeyPropertyName );
			if ( mapKeyAttribute instanceof ToOneAttributeMapping ) {
				final ToOneAttributeMapping toOne = (ToOneAttributeMapping) mapKeyAttribute;
				final NavigablePath mapKeyPropertyPath = navigablePath.append( mapKeyPropertyName );
				final TableGroupJoin tableGroupJoin = toOne.createTableGroupJoin(
						mapKeyPropertyPath,
						elementTableGroup,
						null,
						null,
						fetched,
						addsPredicate,
						aliasBaseGenerator,
						sqlExpressionResolver,
						fromClauseAccess,
						creationContext
				);
				fromClauseAccess.registerTableGroup( mapKeyPropertyPath, tableGroupJoin.getJoinedGroup() );
				return tableGroupJoin;
			}
		}

		return new TableGroupJoin( navigablePath, joinType, elementTableGroup, null );
	}

	@Override
	public TableGroup createRootTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			SqlAstJoinType sqlAstJoinType,
			boolean fetched,
			Consumer<Predicate> predicateConsumer,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			FromClauseAccess fromClauseAccess,
			SqlAstCreationContext creationContext) {
		return createTableGroupInternal(
				true,
				navigablePath,
				fetched,
				explicitSourceAlias,
				aliasBaseGenerator.createSqlAliasBase( getSqlAliasStem() ),
				sqlExpressionResolver,
				creationContext
		);
	}

	public TableGroup createAssociatedTableGroup(
			boolean canUseInnerJoins,
			NavigablePath append,
			boolean fetched,
			String sourceAlias,
			SqlAliasBase sqlAliasBase,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		return createTableGroupInternal( canUseInnerJoins, append, fetched, sourceAlias, sqlAliasBase, sqlExpressionResolver, creationContext );
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
}
