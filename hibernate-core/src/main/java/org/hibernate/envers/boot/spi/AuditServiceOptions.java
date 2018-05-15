/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.spi;

import org.hibernate.envers.RevisionListener;
import org.hibernate.envers.strategy.AuditStrategy;

/**
 * Aggregator of options for the Envers {@code AuditService}.
 *
 * @author Chris Cranford
 * @since 6.0
 */
public interface AuditServiceOptions {

	boolean isRevisionOnCollectionChangeEnabled();

	boolean isDoNotAuditOptimisticLockingFieldEnabled();

	boolean isStoreDataAtDeleteEnabled();

	boolean isTrackEntitiesChangedInRevisionEnabled();

	boolean isGlobalWithModifiedFlagEnabled();

	boolean isGlobalLegacyRelationTargetNotFoundEnabled();

	boolean hasGlobalWithModifiedFlag();

	boolean isUseRevisionEntityWithNativeIdEnabled();

	boolean isCascadeDeleteRevisionEnabled();

	boolean isAllowIdentifierReuseEnabled();

	String getDefaultSchemaName();

	String getDefaultCatalogName();

	String getCorrelatedSubqueryOperator();

	String getModifiedFlagSuffix();

	Class<? extends RevisionListener> getRevisionListenerClass();

	String getOriginalIdPropName();

	String getRevisionFieldName();

	boolean isRevisionEndTimestampEnabled();

	boolean isRevisionEndTimestampLegacyBehaviorEnabled();

	String getRevisionEndTimestampFieldName();

	boolean isNumericRevisionEndTimestampEnabled();

	String getRevisionNumberPath();

	String getRevisionTypePropName();

	String getRevisionTypePropType();

	String getRevisionInfoEntityName();

	String getRevisionEndFieldName();

	String getEmbeddableSetOrdinalPropertyName();

	AuditStrategy getAuditStrategy();
}
