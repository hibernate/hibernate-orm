/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.audit.spi.AuditEntityLoader;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Metadata about audit log tables for entities and collections enabled for audit logging.
 *
 * @author Gavin King
 * @see org.hibernate.annotations.Audited
 * @since 7.4
 */
@Incubating
public interface AuditMapping extends AuxiliaryMapping {

	/**
	 * Get the changeset ID selectable mapping for the given original table.
	 */
	SelectableMapping getChangesetIdMapping(String originalTableName);

	/**
	 * Get the modification type selectable mapping for the given original table,
	 * or {@code null} if the table does not carry a modification type column.
	 */
	@Nullable
	SelectableMapping getModificationTypeMapping(String originalTableName);

	/**
	 * Get the invalidating changeset selectable mapping for the given original table,
	 * or {@code null} if the validity audit strategy is not active.
	 */
	@Nullable
	SelectableMapping getInvalidatingChangesetIdMapping(String originalTableName);

	/**
	 * Get the invalidation timestamp selectable mapping for the given original table,
	 * or {@code null} if not configured.
	 */
	@Nullable
	SelectableMapping getInvalidationTimestampMapping(String originalTableName);

	/**
	 * Get the entity loader for single-entity audit queries.
	 */
	AuditEntityLoader getEntityLoader();

	/**
	 * Build the temporal restriction predicate for the given table
	 * with an explicit upper bound expression.
	 * <p>
	 * Used by {@link org.hibernate.audit.spi.AuditEntityLoader}
	 * implementations to build audit-specific load plans.
	 *
	 * @param includeDeletions if {@code true}, omit the {@code REVTYPE <> DEL} filter
	 */
	Predicate createRestriction(
			TableGroupProducer tableGroupProducer,
			TableReference tableReference,
			List<SelectableMapping> keySelectables,
			SqlAliasBaseGenerator sqlAliasBaseGenerator,
			String originalTableName,
			Expression upperBound,
			boolean includeDeletions);
}
