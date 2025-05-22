/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.internal;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.BasicValuedCollectionPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.JoinedSubclassEntityPersister;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.ForUpdateClauseStrategy;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.select.QuerySpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * ForUpdateClauseStrategy implementation largely following the SQL standard.
 *
 * @author Steve Ebersole
 */
public class StandardForUpdateClauseStrategy implements ForUpdateClauseStrategy {
	private final Dialect dialect;
	private final RowLockStrategy rowLockStrategy;
	private final PessimisticLockKind lockKind;
	private final Locking.Scope lockingScope;
	private final int timeout;

	private Set<TableGroup> rootsToLock;
	private Set<TableGroupJoin> joinsToLock;

	public StandardForUpdateClauseStrategy(
			Dialect dialect,
			RowLockStrategy rowLockStrategy,
			LockMode lockMode,
			PessimisticLockKind lockKind,
			Locking.Scope lockingScope,
			int timeout) {
		assert lockKind != PessimisticLockKind.NONE;
		assert lockMode.isPessimistic();

		this.dialect = dialect;
		this.rowLockStrategy = rowLockStrategy;
		this.lockKind = lockKind;
		this.lockingScope = lockingScope;
		this.timeout = timeout;
	}

	@Override
	public void registerRoot(TableGroup root) {
		if ( rowLockStrategy == RowLockStrategy.NONE ) {
			// no need to collect these
			return;
		}

		if ( rootsToLock == null ) {
			rootsToLock = new HashSet<>();
		}
		rootsToLock.add( root );
	}

	@Override
	public void registerJoin(TableGroupJoin join) {
		if ( rowLockStrategy == RowLockStrategy.NONE ) {
			// no need to collect these
			return;
		}
		else if ( lockingScope == Locking.Scope.INCLUDE_COLLECTIONS ) {
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
		if ( joinsToLock == null ) {
			joinsToLock = new LinkedHashSet<>();
		}
		joinsToLock.add( join );
	}

	@Override
	public boolean containsOuterJoins() {
		for ( TableGroup tableGroup : rootsToLock ) {
			if ( tableGroup.getModelPart() instanceof JoinedSubclassEntityPersister ) {
				// inherently has outer joins
				return true;
			}
		}

		if ( joinsToLock == null ) {
			return false;
		}
		for ( TableGroupJoin tableGroupJoin : joinsToLock ) {
			final TableGroup joinedGroup = tableGroupJoin.getJoinedGroup();
			if ( tableGroupJoin.isInitialized()
				&& tableGroupJoin.getJoinType() != SqlAstJoinType.INNER
				&& !joinedGroup.isVirtual() ) {
				return true;
			}
			if ( joinedGroup.getModelPart() instanceof JoinedSubclassEntityPersister ) {
				// inherently has outer joins
				return true;
			}
		}

		return false;
	}

	@Override
	public void render(SqlAppender sqlAppender) {
		renderLockFragment( dialect, lockKind, timeout, rowLockStrategy, sqlAppender );
		renderResultSetOptions( dialect, sqlAppender );
	}

	protected void renderLockFragment(
			Dialect dialect,
			PessimisticLockKind lockKind,
			int timeout,
			RowLockStrategy rowLockStrategy,
			SqlAppender sqlAppender) {
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

	protected void renderResultSetOptions(Dialect dialect, SqlAppender sqlAppender) {
		// hook for Derby
	}

	private void collectLockItems(TableGroup tableGroup, List<String> lockItems) {
		if ( rowLockStrategy == RowLockStrategy.TABLE ) {
			addTableAliases( tableGroup, lockItems );
		}
		else if ( rowLockStrategy == RowLockStrategy.COLUMN ) {
			addColumnRefs( tableGroup, lockItems );
		}
		else if ( rowLockStrategy == RowLockStrategy.COLUMN_NAME ) {
			addColumnNames( tableGroup, lockItems );
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
		Collections.addAll( lockItems, determineKeyColumnRefs( tableGroup ) );
	}

	private String[] determineKeyColumnRefs(TableGroup tableGroup) {
		final String[] result = determineKeyColumnNames( tableGroup.getModelPart() );
		final String tableAlias = tableGroup.getPrimaryTableReference().getIdentificationVariable();
		for ( int i = 0; i < result.length; i++ ) {
			result[i] = tableAlias + "." + result[i];
		}
		return result;
	}

	private String[] determineKeyColumnNames(ModelPart modelPart) {
		if ( modelPart instanceof EntityPersister entityPersister ) {
			return entityPersister.getIdentifierColumnNames();
		}
		else if ( modelPart instanceof PluralAttributeMapping pluralAttributeMapping ) {
			// todo : seems like this ought to return the column name(s)
			//  	to then be qualified with the table alias
			return pluralAttributeMapping.getCollectionDescriptor().getKeyColumnAliases( null );
		}
		else if ( modelPart instanceof EntityAssociationMapping entityAssociationMapping ) {
			return determineKeyColumnNames( entityAssociationMapping.getAssociatedEntityMappingType() );
		}
		else {
			return null;
		}
	}

	private void addColumnNames(TableGroup tableGroup, List<String> lockItems) {
		final ModelPart keyColumnPart = determineKeyPart( tableGroup.getModelPart() );
		if ( keyColumnPart == null ) {
			throw new HibernateException( "Could not determine ModelPart defining key columns - " + tableGroup.getModelPart() );
		}
		keyColumnPart.forEachSelectable( (selectionIndex, selectableMapping) -> {
			if ( !selectableMapping.isFormula() ) {
				lockItems.add( selectableMapping.getSelectableName() );
			}
		} );
	}

	private ModelPart determineKeyPart(ModelPart modelPart) {
		if ( modelPart instanceof EntityPersister entityPersister ) {
			return entityPersister.getIdentifierMapping();
		}
		else if ( modelPart instanceof PluralAttributeMapping pluralAttributeMapping ) {
			return pluralAttributeMapping.getKeyDescriptor();
		}
		else if ( modelPart instanceof EntityAssociationMapping entityAssociationMapping ) {
			return entityAssociationMapping.getForeignKeyDescriptor();
		}
		else {
			return null;
		}
	}

	public static ForUpdateClauseStrategy strategy(Dialect dialect, QuerySpec querySpec, LockOptions lockOptions) {
		return strategy(
				dialect,
				querySpec,
				lockOptions,
				(dialect1, rowLockStrategy, lockMode, lockKind, lockScope, timeout) -> new StandardForUpdateClauseStrategy(
						dialect,
						rowLockStrategy,
						lockMode,
						lockKind,
						lockScope,
						timeout
				)
		);
	}

	@FunctionalInterface
	public interface ForUpdateClauseStrategyProducer {
		ForUpdateClauseStrategy produceStrategy(
				Dialect dialect,
				RowLockStrategy rowLockStrategy,
				LockMode lockMode,
				PessimisticLockKind lockKind,
				Locking.Scope lockScope,
				int timeout);
	}

	public static ForUpdateClauseStrategy strategy(
			Dialect dialect,
			QuerySpec querySpec,
			LockOptions lockOptions,
			ForUpdateClauseStrategyProducer producer) {
		if ( lockOptions == null ) {
			return NoOpForUpdateClauseStrategy.NO_OP_STRATEGY;
		}

		final LockMode lockMode = lockOptions.getLockMode();
		final PessimisticLockKind lockKind = PessimisticLockKind.interpret( lockMode );
		if ( lockKind == PessimisticLockKind.NONE ) {
			return NoOpForUpdateClauseStrategy.NO_OP_STRATEGY;
		}

		final RowLockStrategy rowLockStrategy;
		switch ( lockKind ) {
			case SHARE -> rowLockStrategy = dialect.getReadRowLockStrategy();
			case UPDATE -> rowLockStrategy = dialect.getWriteRowLockStrategy();
			default -> throw new IllegalStateException( "Should never happen due to checks above" );
		}

		return producer.produceStrategy(
				dialect,
				rowLockStrategy,
				lockMode,
				lockKind,
				lockOptions.getScope(),
				lockOptions.getTimeOut()
		);
	}
}
