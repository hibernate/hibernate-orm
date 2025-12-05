/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.internal;

import jakarta.persistence.Timeout;
import org.hibernate.AssertionFailure;
import org.hibernate.LockOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.EntityTableMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.exec.internal.lock.LockingTableGroup;
import org.hibernate.sql.model.TableMapping;

import java.util.ArrayList;
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
public class StandardLockingClauseStrategy extends AbstractLockingClauseStrategy {
	private final Dialect dialect;
	private final RowLockStrategy rowLockStrategy;
	private final PessimisticLockKind lockKind;
	private final Timeout timeout;

	private boolean queryHasOuterJoins = false;

	private Set<TableGroup> rootsToLock;
	private Set<TableGroupJoin> joinsToLock;

	public StandardLockingClauseStrategy(
			Dialect dialect,
			PessimisticLockKind lockKind,
			RowLockStrategy rowLockStrategy,
			LockOptions lockOptions,
			Set<NavigablePath> rootsForLocking) {
		super( lockOptions.getScope(), rootsForLocking );

		assert lockKind != PessimisticLockKind.NONE;

		this.dialect = dialect;
		this.rowLockStrategy = rowLockStrategy;
		this.lockKind = lockKind;
		this.timeout = lockOptions.getTimeout();
	}

	@Override
	public boolean registerRoot(TableGroup root) {
		if ( !queryHasOuterJoins ) {
			if ( CollectionHelper.isNotEmpty( root.getTableReferenceJoins() ) ) {
				// joined inheritance and/or secondary tables - inherently has outer joins
				queryHasOuterJoins = true;
			}
		}

		return super.registerRoot( root );
	}

	@Override
	protected void trackRoot(TableGroup root) {
		super.trackRoot( root );
		if ( rootsToLock == null ) {
			rootsToLock = new HashSet<>();
		}
		rootsToLock.add( root );
	}

	@Override
	public boolean registerJoin(TableGroupJoin join) {
		checkForOuterJoins( join );
		return super.registerJoin( join );
	}

	@Override
	protected void trackJoin(TableGroupJoin join) {
		super.trackJoin( join );
		if ( joinsToLock == null ) {
			joinsToLock = new LinkedHashSet<>();
		}
		joinsToLock.add( join );
	}

	private void checkForOuterJoins(TableGroupJoin join) {
		if ( queryHasOuterJoins ) {
			// perf out
			return;
		}
		queryHasOuterJoins = hasOuterJoin( join );
	}

	private boolean hasOuterJoin(TableGroupJoin join) {
		final TableGroup joinedGroup = join.getJoinedGroup();
		if ( join.isInitialized()
			&& join.getJoinType() != SqlAstJoinType.INNER
			&& !joinedGroup.isVirtual() ) {
			return true;
		}
		if ( !CollectionHelper.isEmpty( joinedGroup.getTableReferenceJoins() ) ) {
			for ( TableReferenceJoin tableReferenceJoin : joinedGroup.getTableReferenceJoins() ) {
				if ( tableReferenceJoin.getJoinType() != SqlAstJoinType.INNER ) {
					return true;
				}
			}
		}
		return false;
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

	protected void renderLockFragment(SqlAppender sqlAppender) {
		final String fragment;
		if ( rowLockStrategy == RowLockStrategy.NONE ) {
			fragment = lockKind == PessimisticLockKind.SHARE
					? dialect.getReadLockString( timeout )
					: dialect.getWriteLockString( timeout );
		}
		else if ( CollectionHelper.isEmpty( rootsToLock )
				&& CollectionHelper.isEmpty( joinsToLock ) ) {
			// this might happen with locking and scalar queries.  e.g.
			// 		session.createQuery( "select p.unitCost * .049 from Product p" )
			//				.setLockMode(...)
			//
			// the spec says:
			//   > (if) the query returns scalar data ..., the underlying database rows will be locked
			//
			// so we use a simple `for update`, with no `of`.  aka, we treat it the same as RowLockStrategy.NONE
			assert CollectionHelper.isEmpty( rootsForLocking );
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
		if ( rowLockStrategy == null ) {
			return "";
		}

		final List<String> lockItems = new ArrayList<>();
		if ( rootsToLock != null ) {
			for ( TableGroup root : rootsToLock ) {
				collectLockItems( root, lockItems );
			}
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
		final String[] keyColumns = determineKeyColumnNames( tableGroup );
		final String tableAlias = tableGroup.getPrimaryTableReference().getIdentificationVariable();
		for ( int i = 0; i < keyColumns.length; i++ ) {
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

	private String[] determineKeyColumnNames(TableGroup tableGroup) {
		if ( tableGroup instanceof LockingTableGroup lockingTableGroup ) {
			return extractColumnNames( lockingTableGroup.getKeyColumnMappings() );
		}
		else if ( tableGroup.getModelPart() instanceof EntityPersister entityPersister ) {
			return entityPersister.getIdentifierColumnNames();
		}
		else if ( tableGroup.getModelPart() instanceof PluralAttributeMapping pluralAttributeMapping ) {
			return extractColumnNames( pluralAttributeMapping.getKeyDescriptor() );
		}
		else if ( tableGroup.getModelPart() instanceof EntityAssociationMapping entityAssociationMapping ) {
			return extractColumnNames( entityAssociationMapping.getAssociatedEntityMappingType().getIdentifierMapping() );
		}
		else {
			throw new AssertionFailure( "Unable to determine columns for locking" );
		}
	}

	private static String[] extractColumnNames(SelectableMappings keyColumnMappings) {
		if ( keyColumnMappings.getJdbcTypeCount() == 1 ) {
			return new String[] { keyColumnMappings.getSelectable( 0 ).getSelectableName() };
		}

		final String[] results = new String[ keyColumnMappings.getJdbcTypeCount() ];
		keyColumnMappings.forEachSelectable( (selectionIndex, selectableMapping) -> {
			results[selectionIndex] = selectableMapping.getSelectableName();
		} );
		return results;
	}
}
