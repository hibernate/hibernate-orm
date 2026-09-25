package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;

import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.audit.spi.AuditEntityLoader;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.from.TableGroupProducer;
import org.hibernate.sql.ast.spi.query.from.TableReference;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;

import jakarta.annotation.Nullable;

/**
 * Metadata about audit log tables for entities and collections enabled for audit logging.
 *
 * @author Gavin King
 * @see org.hibernate.annotations.Audited
 * @since 7.4
 */
@Incubating(since = "5.4")
public interface AuditMapping extends AuxiliaryMapping {

	/**
	 * Get the changeset ID selectable mapping for the given original table.
	 */
	@Nonnull
	SelectableMapping getChangesetIdMapping(@Nonnull String originalTableName);

	/**
	 * Get the modification type selectable mapping for the given original table,
	 * or {@code null} if the table does not carry a modification type column.
	 */
	@Nullable
	SelectableMapping getModificationTypeMapping(@Nonnull String originalTableName);

	/**
	 * Get the invalidating changeset selectable mapping for the given original table,
	 * or {@code null} if the validity audit strategy is not active.
	 */
	@Nullable
	SelectableMapping getInvalidatingChangesetIdMapping(@Nonnull String originalTableName);

	/**
	 * Get the entity loader for single-entity audit queries.
	 */
	@Nonnull
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
	@Nonnull
	Predicate createRestriction(
			@Nonnull TableGroupProducer tableGroupProducer,
			@Nonnull TableReference tableReference,
			@Nonnull List<SelectableMapping> keySelectables,
			@Nonnull SqlAliasBaseGenerator sqlAliasBaseGenerator,
			@Nonnull String originalTableName,
			@Nonnull Expression upperBound,
			boolean includeDeletions);
}
