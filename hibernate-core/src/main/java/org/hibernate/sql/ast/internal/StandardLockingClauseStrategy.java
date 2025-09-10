/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.internal;

import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.internal.BasicValuedCollectionPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.EntityTableMapping;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.LockingClauseStrategy;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.model.TableMapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * LockingClauseStrategy implementation for dialects with support for
 * {@code for share (of)} and {@code for update (of)} clauses.
 *
 * @author Steve Ebersole
 */
public class StandardLockingClauseStrategy implements LockingClauseStrategy {
	private final Dialect dialect;
	private final RowLockStrategy rowLockStrategy;
	private final PessimisticLockKind lockKind;
	private final Locking.Scope lockingScope;
	private final int timeout;

	/**
	 * @implNote Tracked separately from {@linkplain #rootsToLock} and
	 * {@linkplain #joinsToLock} to help answer {@linkplain #containsOuterJoins()}
	 * for {@linkplain RowLockStrategy#NONE cases} where we otherwise don't need to
	 * track the tables, allowing to avoid the overhead of the Sets.  There is a
	 * slight trade-off in that we need to inspect the from-elements to make that
	 * determination when we might otherwise not need to - memory versus cpu.
	 */
	private boolean queryHasOuterJoins = false;

	private Set<TableGroup> rootsToLock;
	private Set<TableGroupJoin> joinsToLock;

	public StandardLockingClauseStrategy(
			Dialect dialect,
			PessimisticLockKind lockKind,
			RowLockStrategy rowLockStrategy,
			LockOptions lockOptions) {
		assert lockKind != PessimisticLockKind.NONE;

		this.dialect = dialect;
		this.rowLockStrategy = rowLockStrategy;
		this.lockKind = lockKind;
		this.lockingScope = lockOptions.getScope();
		this.timeout = lockOptions.getTimeout().milliseconds();
	}

	@Override
	public void registerRoot(TableGroup root) {
		if ( !queryHasOuterJoins && !dialect.supportsOuterJoinForUpdate() ) {
			if ( CollectionHelper.isNotEmpty( root.getTableReferenceJoins() ) ) {
				// joined inheritance and/or secondary tables - inherently has outer joins
				queryHasOuterJoins = true;
			}
		}

		if ( rowLockStrategy != RowLockStrategy.NONE ) {
			if ( rootsToLock == null ) {
				rootsToLock = new HashSet<>();
			}
			rootsToLock.add( root );
		}
	}

	@Override
	public void registerJoin(TableGroupJoin join) {
		if ( lockingScope == Locking.Scope.INCLUDE_COLLECTIONS ) {
			// if the TableGroup is an owned (aka, non-inverse) collection,
			// and we are to lock collections, track it
			if ( join.getJoinedGroup().getModelPart() instanceof PluralAttributeMapping attrMapping ) {
				if ( !attrMapping.getCollectionDescriptor().isInverse() ) {
					// owned collection
					if ( attrMapping.getElementDescriptor() instanceof BasicValuedCollectionPart ) {
						// an element-collection
						trackJoin( join );
					}
				}
			}
		}
		else if ( lockingScope == Locking.Scope.INCLUDE_FETCHES ) {
			if ( join.getJoinedGroup().isFetched() ) {
				trackJoin( join );
			}
		}
	}

	private void trackJoin(TableGroupJoin join) {
		if ( !queryHasOuterJoins && !dialect.supportsOuterJoinForUpdate() ) {
			final TableGroup joinedGroup = join.getJoinedGroup();
			if ( join.isInitialized()
				&& join.getJoinType() != SqlAstJoinType.INNER
				&& !joinedGroup.isVirtual() ) {
				queryHasOuterJoins = true;
			}
			else if ( joinedGroup.getModelPart() instanceof EntityPersister entityMapping ) {
				if ( entityMapping.hasMultipleTables() ) {
					// joined inheritance and/or secondary tables - inherently has outer joins
					queryHasOuterJoins = true;
				}
			}
		}

		if ( rowLockStrategy != RowLockStrategy.NONE ) {
			if ( joinsToLock == null ) {
				joinsToLock = new LinkedHashSet<>();
			}
			joinsToLock.add( join );
		}
	}

	@Override
	public boolean containsOuterJoins() {
		return queryHasOuterJoins;
	}

	@Override
	public void render(SqlAppender sqlAppender) {
		renderLockFragment( sqlAppender );
		renderResultSetOptions( sqlAppender );
	}

	@Override
	public Collection<TableGroup> getRootsToLock() {
		return rootsToLock;
	}

	@Override
	public Collection<TableGroupJoin> getJoinsToLock() {
		return joinsToLock;
	}

	protected void renderLockFragment(SqlAppender sqlAppender) {
		final String fragment;
		if ( rowLockStrategy == RowLockStrategy.NONE ) {
			fragment = lockKind == PessimisticLockKind.SHARE
					? dialect.getReadLockString( timeout )
					: dialect.getWriteLockString( timeout );
		}
		else {
			final String lockItemsFragment = collectLockItems();
			fragment = lockKind == PessimisticLockKind.SHARE
					? dialect.getReadLockString( lockItemsFragment, timeout )
					: dialect.getWriteLockString( lockItemsFragment, timeout );
		}
		sqlAppender.append( fragment );
	}

	private String collectLockItems() {
		final List<String> lockItems = new ArrayList<>();
		for ( TableGroup root : rootsToLock ) {
			collectLockItems( root, lockItems );
		}
		if ( joinsToLock != null ) {
			for ( TableGroupJoin join : joinsToLock ) {
				collectLockItems( join.getJoinedGroup(), lockItems );
			}
		}

		final StringBuilder buffer = new StringBuilder();
		boolean first = true;
		for ( String lockItem : lockItems ) {
			if ( first ) {
				first = false;
			}
			else {
				buffer.append( ',' );
			}
			buffer.append( lockItem );
		}

		return buffer.toString();
	}

	protected void renderResultSetOptions(SqlAppender sqlAppender) {
		// hook for Derby
	}

	private void collectLockItems(TableGroup tableGroup, List<String> lockItems) {
		if ( rowLockStrategy == RowLockStrategy.TABLE ) {
			addTableAliases( tableGroup, lockItems );
		}
		else if ( rowLockStrategy == RowLockStrategy.COLUMN ) {
			addColumnRefs( tableGroup, lockItems );
		}
	}

	private void addTableAliases(TableGroup tableGroup, List<String> lockItems) {
		final String tableAlias = tableGroup.getPrimaryTableReference().getIdentificationVariable();
		lockItems.add( tableAlias );

		final List<TableReferenceJoin> tableReferenceJoins = tableGroup.getTableReferenceJoins();
		if ( CollectionHelper.isNotEmpty( tableReferenceJoins ) ) {
			for ( int i = 0; i < tableReferenceJoins.size(); i++ ) {
				lockItems.add( tableReferenceJoins.get(i).getJoinedTableReference().getIdentificationVariable() );
			}
		}
	}

	private void addColumnRefs(TableGroup tableGroup, List<String> lockItems) {
		final String[] keyColumns = determineKeyColumnNames( tableGroup.getModelPart() );
		final String tableAlias = tableGroup.getPrimaryTableReference().getIdentificationVariable();
		for ( int i = 0; i < keyColumns.length; i++ ) {
			// NOTE: in some tests with Oracle, the qualifiers are being applied twice;
			//		still need to track that down.  possibly, unexpected calls to
			//		`Dialect#applyLocksToSql`?
			assert !keyColumns[i].contains( "." );
			lockItems.add( tableAlias + "." + keyColumns[i] );
		}

		final List<TableReferenceJoin> tableReferenceJoins = tableGroup.getTableReferenceJoins();
		if ( CollectionHelper.isNotEmpty( tableReferenceJoins ) ) {
			final EntityPersister entityPersister = determineEntityPersister( tableGroup.getModelPart() );
			for ( int i = 0; i < tableReferenceJoins.size(); i++ ) {
				final TableReferenceJoin tableReferenceJoin = tableReferenceJoins.get( i );
				final NamedTableReference joinedTableReference = tableReferenceJoin.getJoinedTableReference();
				final String tableJoinAlias = joinedTableReference.getIdentificationVariable();
				final TableMapping tableMapping = determineTableMapping( entityPersister, tableReferenceJoin );
				for ( TableDetails.KeyColumn keyColumn : tableMapping.getKeyDetails().getKeyColumns() ) {
					lockItems.add( tableJoinAlias + "." + keyColumn.getColumnName() );
				}
			}
		}
	}

	private TableMapping determineTableMapping(EntityPersister entityPersister, TableReferenceJoin tableReferenceJoin) {
		final NamedTableReference joinedTableReference = tableReferenceJoin.getJoinedTableReference();
		for ( EntityTableMapping tableMapping : entityPersister.getTableMappings() ) {
			if ( joinedTableReference.containsAffectedTableName( tableMapping.getTableName() ) ) {
				return tableMapping;
			}
		}
		for ( EntityMappingType subMappingType : entityPersister.getSubMappingTypes() ) {
			for ( EntityTableMapping tableMapping : subMappingType.getEntityPersister().getTableMappings() ) {
				if ( joinedTableReference.containsAffectedTableName( tableMapping.getTableName() ) ) {
					return tableMapping;
				}
			}
		}
		throw new IllegalArgumentException( "Couldn't find subclass index for joined table reference " + joinedTableReference );
	}

	private EntityPersister determineEntityPersister(ModelPartContainer modelPart) {
		if ( modelPart instanceof EntityPersister entityPersister ) {
			return entityPersister;
		}
		else if ( modelPart instanceof PluralAttributeMapping pluralAttributeMapping ) {
			return pluralAttributeMapping.getCollectionDescriptor().getElementPersister();
		}
		else if ( modelPart instanceof EntityAssociationMapping entityAssociationMapping ) {
			return entityAssociationMapping.getAssociatedEntityMappingType().getEntityPersister();
		}
		else {
			throw new IllegalArgumentException( "Expected table group with table joins to have an entity typed model part but got: " + modelPart );
		}
	}

	private String[] determineKeyColumnNames(ModelPart modelPart) {
		if ( modelPart instanceof EntityPersister entityPersister ) {
			return entityPersister.getIdentifierColumnNames();
		}
		else if ( modelPart instanceof PluralAttributeMapping pluralAttributeMapping ) {
			final ForeignKeyDescriptor keyDescriptor = pluralAttributeMapping.getKeyDescriptor();
			final ValuedModelPart keyPart = keyDescriptor.getKeyPart();
			if ( keyPart.getJdbcTypeCount() == 1 ) {
				return new String[] { keyPart.getSelectable( 0 ).getSelectableName() };
			}

			final ArrayList<String> results = CollectionHelper.arrayList( keyPart.getJdbcTypeCount() );
			keyPart.forEachSelectable( (index, selectable) -> {
				if ( !selectable.isFormula() ) {
					results.add( selectable.getSelectableName() );
				}
			} );
			return results.toArray( new String[0] );
		}
		else if ( modelPart instanceof EntityAssociationMapping entityAssociationMapping ) {
			return determineKeyColumnNames( entityAssociationMapping.getAssociatedEntityMappingType() );
		}
		else {
			return null;
		}
	}
}
