/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.internal;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.Timeouts;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.BasicValuedCollectionPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.spi.ForUpdateClauseStrategy;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.select.QuerySpec;

import java.util.ArrayList;
import java.util.Collections;
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

	private Set<TableGroup> tableGroupsToLock;

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
	public void register(TableGroup tableGroup, boolean isRoot) {
		if ( rowLockStrategy == RowLockStrategy.NONE ) {
			// no need to collect these
			return;
		}

		if ( isRoot ) {
			assert tableGroup.isRealTableGroup();

			// we always want to lock tables which are part of the roots
			trackTableGroup( tableGroup );
		}
		else if ( lockingScope == Locking.Scope.INCLUDE_COLLECTIONS ) {
			// if the TableGroup is an owned (aka, non-inverse) collection,
			// and we are to lock collections, track it
			if ( tableGroup.getModelPart() instanceof PluralAttributeMapping attrMapping ) {
				if ( !attrMapping.getCollectionDescriptor().isInverse() ) {
					// owned collection
					if ( attrMapping.getElementDescriptor() instanceof BasicValuedCollectionPart ) {
						// an element-collection
						trackTableGroup( tableGroup );
					}
				}
			}
		}
		else if ( lockingScope == Locking.Scope.INCLUDE_FETCHES ) {
			if ( tableGroup.isFetched() ) {
				trackTableGroup( tableGroup );
			}
		}
	}

	private void trackTableGroup(TableGroup tableGroup) {
		if ( tableGroupsToLock == null ) {
			tableGroupsToLock = new LinkedHashSet<>();
		}
		tableGroupsToLock.add( tableGroup );
	}


	@Override
	public void render(SqlAppender sqlAppender) {
		renderLockTerm( dialect, lockKind, timeout, sqlAppender );
		renderRowLocking( rowLockStrategy, tableGroupsToLock, sqlAppender );
		renderResultSetOptions( dialect, sqlAppender );
		renderLockedRowHandling( timeout, sqlAppender );
	}

	protected void renderLockTerm(
			Dialect dialect,
			PessimisticLockKind lockKind,
			int timeout,
			SqlAppender sqlAppender) {
		final String term = lockKind == PessimisticLockKind.SHARE
				? dialect.getReadLockString( timeout )
				: dialect.getWriteLockString( timeout );
		sqlAppender.append( term );
	}

	protected void renderRowLocking(RowLockStrategy rowLockStrategy, Set<TableGroup> tableGroupsToLock, SqlAppender sqlAppender) {
		if ( rowLockStrategy == RowLockStrategy.NONE ) {
			return;
		}

		assert tableGroupsToLock != null && !tableGroupsToLock.isEmpty();

		sqlAppender.append( " of " );

		final List<String> lockItems = new ArrayList<>();
		for ( TableGroup tableGroup : tableGroupsToLock ) {
			collectLockItems( tableGroup, lockItems );
		}

		boolean first = true;
		for ( String lockItem : lockItems ) {
			if ( first ) {
				first = false;
			}
			else {
				sqlAppender.appendSql( ',' );
			}
			sqlAppender.appendSql( lockItem );
		}
	}

	protected void renderResultSetOptions(Dialect dialect, SqlAppender sqlAppender) {
		// todo : hook for derby
	}

	protected void renderLockedRowHandling(int timeout, SqlAppender sqlAppender) {
		if ( timeout == Timeouts.NO_WAIT_MILLI ) {
			if ( dialect.supportsNoWait() ) {
				sqlAppender.append( dialect.getForUpdateNowaitString() );
			}
		}
		else if ( timeout == Timeouts.SKIP_LOCKED_MILLI ) {
			if ( dialect.supportsSkipLocked() ) {
				sqlAppender.append( dialect.getForUpdateSkipLockedString() );
			}
		}
		else if ( timeout > 0 ) {
			if ( dialect.supportsLockTimeouts() ) {
				sqlAppender.append( " wait " );
				sqlAppender.append( Integer.toString( Timeouts.getTimeoutInSeconds( timeout ) ) );
			}
		}
	}

	private void collectLockItems(TableGroup tableGroup, List<String> lockItems) {
		if ( rowLockStrategy == RowLockStrategy.TABLE ) {
			addTableAliases( tableGroup, lockItems );
		}
		else if ( rowLockStrategy == RowLockStrategy.COLUMN ) {
			addColumnRefs( tableGroup, lockItems );
		}
	}

	private void addColumnRefs(TableGroup tableGroup, List<String> lockItems) {
		Collections.addAll( lockItems, determineKeyColumnRefs( tableGroup ) );
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
			return pluralAttributeMapping.getCollectionDescriptor().getKeyColumnAliases( null );
		}
		else if ( modelPart instanceof EntityAssociationMapping entityAssociationMapping ) {
			return determineKeyColumnNames( entityAssociationMapping.getAssociatedEntityMappingType() );
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
