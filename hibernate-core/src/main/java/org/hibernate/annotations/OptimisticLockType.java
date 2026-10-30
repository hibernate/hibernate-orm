package org.hibernate.annotations;

/**
 * Enumerates automatic optimistic checking strategies for entity mutations.
 * These strategies do not schedule validation of unchanged entities.
 * Explicit {@link org.hibernate.LockMode#OPTIMISTIC} validation requires
 * a versioned entity.
 *
 * @see OptimisticLocking
 *
 * @author Emmanuel Bernard
 */
public enum OptimisticLockType {
	/**
	 * Disable optimistic mutation checks, even when a version attribute exists.
	 */
	NONE,
	/**
	 * Optimistic locking using a dedicated timestamp column or
	 * {@linkplain jakarta.persistence.Version version column}.
	 * This is the usual strategy.
	 * <p>
	 * Entity mutations include the expected version in their optimistic
	 * predicates. A missing matching row indicates a stale version or a deleted
	 * entity. Bulk mutations do not automatically perform these per-entity checks.
	 *
	 * @see jakarta.persistence.Version
	 */
	VERSION,
	/**
	 * Optimistic mutation checking based on eligible previously loaded values
	 * of <em>dirty</em> attributes for updates.
	 * <p>
	 * An entity {@code update} compares those values in the {@code where}
	 * restriction, respecting versioning exclusions. Deletes have no ordinary
	 * dirty-property set and may compare eligible loaded values instead.
	 * Requires available original loaded state and dynamic updates, and cannot
	 * be combined with a {@link jakarta.persistence.Version} attribute.
	 */
	DIRTY,
	/**
	 * Optimistic mutation checking based on <em>all</em> eligible previously
	 * loaded values of the entity.
	 * <p>
	 * Entity updates and deletes compare those values in their optimistic
	 * predicates, respecting versioning exclusions. Requires available original
	 * loaded state and dynamic updates, and cannot be combined with a
	 * {@link jakarta.persistence.Version} attribute.
	 */
	ALL
}
