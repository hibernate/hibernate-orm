package org.hibernate.metamodel.mapping.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.audit.AuditLog;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.from.PluralTableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroup;

import java.util.function.Supplier;

/**
 * Decides where an all-changesets session reads current state instead of audit rows.
 * <p>
 * An entity without history is always its current version, and so is an audited entity
 * read from its current table. A current version can only refer to current versions:
 * its to-one associations and collections resolve to the current state of their targets.
 */
public final class CurrentState {

	private CurrentState() {
	}

	/**
	 * Whether the entity has versions other than its current one.
	 */
	public static boolean hasHistory(@Nonnull EntityMappingType entityMappingType) {
		return entityMappingType.getAuditMapping() != null
			|| entityMappingType.getTemporalMapping() != null;
	}

	/**
	 * Whether the collection has states other than its current one: it is audited
	 * or temporal, or its elements or keys are.
	 */
	public static boolean hasHistory(@Nonnull PluralAttributeMapping collection) {
		return collection.getAuditMapping() != null
			|| collection.getTemporalMapping() != null
			|| collection.getElementDescriptor() instanceof EntityCollectionPart element
					&& hasHistory( element.getAssociatedEntityMappingType() )
			|| collection.getIndexDescriptor() instanceof EntityCollectionPart index
					&& hasHistory( index.getAssociatedEntityMappingType() );
	}

	/**
	 * Whether the given table group holds current state in an all-changesets session:
	 * it is an entity without history, a temporal root read as current, or a to-one or
	 * collection element reached from current state. Outside an all-changesets session
	 * nothing is singled out as current state.
	 */
	public static boolean readsCurrentState(
			@Nullable TableGroup tableGroup,
			@Nonnull SqlAstCreationState creationState) {
		return creationState.getLoadQueryInfluencers().isAllChangesetsSession()
			&& holdsCurrentState( tableGroup, creationState );
	}

	private static boolean holdsCurrentState(
			@Nullable TableGroup tableGroup,
			@Nonnull SqlAstCreationState creationState) {
		final var owner = associationOwner( tableGroup, creationState );
		if ( owner != null && owner.getModelPart() instanceof EntityValuedModelPart entityPart ) {
			final var entityMappingType = entityPart.getEntityMappingType();
			if ( !hasHistory( entityMappingType ) ) {
				return true;
			}
			else if ( entityPart instanceof ToOneAttributeMapping ) {
				return holdsCurrentState( parent( owner, creationState ), creationState );
			}
			else if ( entityPart instanceof EntityCollectionPart ) {
				// an element is current when the owner of its collection is
				final var collection = parent( owner, creationState );
				return collection != null
					&& holdsCurrentState( parent( collection, creationState ), creationState );
			}
			else {
				// a temporal root is current in an all-revisions query, an audited root never is
				return entityMappingType.getAuditMapping() == null
					&& creationState.getLoadQueryInfluencers().isAllRevisions();
			}
		}
		return false;
	}

	/**
	 * The table group of the entity holding an association navigated from the given
	 * table group: the entity containing an embeddable, the element of a collection
	 * of entities, or the owner of a collection of embeddables.
	 */
	public static @Nullable TableGroup associationOwner(
			@Nullable TableGroup tableGroup,
			@Nonnull SqlAstCreationState creationState) {
		var owner = tableGroup;
		while ( owner != null ) {
			if ( owner instanceof PluralTableGroup pluralTableGroup ) {
				final var element = pluralTableGroup.getElementTableGroup();
				if ( element != null && element.getModelPart() instanceof EntityValuedModelPart ) {
					return element;
				}
				// an embeddable element belongs to the owner of its collection
				owner = parent( owner, creationState );
			}
			else if ( owner.getModelPart() instanceof EmbeddableValuedModelPart ) {
				owner = parent( owner, creationState );
			}
			else {
				return owner;
			}
		}
		return null;
	}

	/**
	 * In an all-revisions query, the revision column of the audit snapshot owning the
	 * association with the given path, or the collection whose element has the given
	 * path, which bounds the state of the target or element. A snapshot reached by an
	 * association is itself the snapshot as of its owner's revision, however it is
	 * joined, even if its own row is older. It is {@code null} when the owner is current
	 * state, and when the owner is a temporal snapshot which the dialect reads with
	 * {@code AS OF}, which cannot take a column.
	 */
	public static @Nullable ColumnReference ownerRevision(
			@Nonnull NavigablePath navigablePath,
			@Nonnull SqlAstCreationState creationState) {
		if ( !creationState.getLoadQueryInfluencers().isAllRevisions() ) {
			return null;
		}
		final var associationPath = isElementPath( navigablePath ) ? navigablePath.getParent() : navigablePath;
		final var ownerPath = associationPath == null ? null : associationPath.getParent();
		// the entity holding the association, through any embeddable
		final var owner = ownerPath == null ? null
				: associationOwner( creationState.getFromClauseAccess().findTableGroup( ownerPath ), creationState );
		if ( owner != null
				&& owner.getModelPart() instanceof EntityValuedModelPart entityPart
				&& !readsCurrentState( owner, creationState ) ) {
			final var entityMappingType = entityPart.getEntityMappingType();
			final var temporalMapping = entityMappingType.getTemporalMapping();
			if ( temporalMapping != null
					&& !temporalMapping.readsHistoryAtOwnerRevision( creationState.getLoadQueryInfluencers() ) ) {
				return null;
			}
			final var revision =
					entityPart instanceof ToOneAttributeMapping || entityPart instanceof EntityCollectionPart
							? ownerRevision( owner.getNavigablePath(), creationState )
							: null;
			if ( revision != null ) {
				return revision;
			}
			final var auditMapping = entityMappingType.getAuditMapping();
			if ( auditMapping != null ) {
				final String table = entityMappingType.getMappedTableDetails().getTableName();
				return new ColumnReference(
						owner.resolveTableReference( auditMapping.resolveTableName( table ) ),
						auditMapping.getChangesetIdMapping( table )
				);
			}
		}
		return null;
	}

	/**
	 * In an all-revisions query, the revision column of the audit snapshot owning a
	 * temporal target or element with the given path, which it is read at. It is
	 * {@code null} when the owner is current state, or when the dialect reads history
	 * with {@code AS OF}, which cannot take a column.
	 *
	 * @see #ownerRevision(NavigablePath, SqlAstCreationState)
	 */
	public static @Nullable ColumnReference ownerRevision(
			@Nonnull TemporalMapping temporalMapping,
			@Nonnull NavigablePath navigablePath,
			@Nonnull SqlAstCreationState creationState) {
		return temporalMapping.readsHistoryAtOwnerRevision( creationState.getLoadQueryInfluencers() )
				? ownerRevision( navigablePath, creationState )
				: null;
	}

	/**
	 * Whether the path is that of the element of a collection.
	 */
	public static boolean isElementPath(@Nonnull NavigablePath navigablePath) {
		return CollectionPart.Nature.ELEMENT.getName().equals( navigablePath.getLocalName() );
	}

	private static @Nullable TableGroup parent(TableGroup tableGroup, SqlAstCreationState creationState) {
		final var parentPath = tableGroup.getNavigablePath().getParent();
		return parentPath == null ? null : creationState.getFromClauseAccess().findTableGroup( parentPath );
	}

	/**
	 * The changeset of the key identifying the given entity as the session reads it:
	 * the changeset it reads at for an entity with history, and {@code null} for
	 * current state.
	 */
	public static @Nullable Object keyChangesetId(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull EntityMappingType entityMappingType) {
		final Object temporalIdentifier = session.getLoadQueryInfluencers().getTemporalIdentifier();
		return temporalIdentifier != null && temporalIdentifier != AuditLog.ALL_CHANGESETS
				&& hasHistory( entityMappingType )
				? temporalIdentifier
				: null;
	}

	/**
	 * Performs a load by id or natural id, which, having no changeset of its own,
	 * reads current state in an all-changesets session.
	 */
	public static <T> T loadByKey(
			@Nonnull EntityMappingType entityMappingType,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Supplier<T> load) {
		final var influencers = session.getLoadQueryInfluencers();
		return influencers.isAllRevisions() && hasHistory( entityMappingType )
				? influencers.withTemporalIdentifier( null, load )
				: load.get();
	}

	/**
	 * How the tables of the target of an association are read.
	 */
	public enum TargetRead {
		/** as the query reads */
		QUERY,
		/** as current state, since the owner of the association is current state */
		CURRENT,
		/** from the history tables of temporal entities, as of the revision of the audit snapshot owning it */
		HISTORY;

		/**
		 * How the target of an association held by the given owner is read: as current
		 * state when it has history and the owner is current state, and from history
		 * tables when it is temporal and read as of the revision of the owner.
		 */
		public static TargetRead of(
				boolean targetHasHistory,
				@Nullable TableGroup owner,
				boolean temporalAtRevision,
				@Nonnull SqlAstCreationState creationState) {
			return targetHasHistory && readsCurrentState( owner, creationState ) ? CURRENT
					: temporalAtRevision ? HISTORY
							: QUERY;
		}

		/**
		 * Performs the action creating or restricting tables of the target.
		 */
		public <T> T read(@Nonnull LoadQueryInfluencers influencers, @Nonnull Supplier<T> action) {
			return switch ( this ) {
				case QUERY -> action.get();
				case CURRENT -> withCurrentState( influencers, action );
				case HISTORY -> influencers.withHistoryTables( true, action );
			};
		}

		/**
		 * Performs the action creating or restricting tables of the target.
		 */
		public void read(@Nonnull LoadQueryInfluencers influencers, @Nonnull Runnable action) {
			read( influencers, () -> {
				action.run();
				return null;
			} );
		}
	}

	/**
	 * Performs the action the way a session without a changeset would.
	 */
	public static <T> T withCurrentState(@Nonnull LoadQueryInfluencers influencers, @Nonnull Supplier<T> action) {
		return influencers.withTemporalIdentifier( null, action );
	}

	/**
	 * Performs the action the way a session without a changeset would.
	 */
	public static void withCurrentState(@Nonnull LoadQueryInfluencers influencers, @Nonnull Runnable action) {
		influencers.withTemporalIdentifier( null, action );
	}
}
