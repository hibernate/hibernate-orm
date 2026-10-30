package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies automatic optimistic checking of mutations to the annotated entity.
 * <p>
 * Optimistic lock checking may detect that an optimistic lock has failed,
 * and that the transaction should be aborted, by comparing either:
 * <ul>
 * <li>the {@linkplain OptimisticLockType#VERSION version or timestamp},
 * <li>the {@linkplain OptimisticLockType#DIRTY dirty fields} of the
 *     entity instance, or
 * <li>{@linkplain OptimisticLockType#ALL all eligible loaded values} of the entity.
 * </ul>
 * <p>
 * An optimistic lock is usually checked by including a restriction in a
 * SQL {@code update} or {@code delete} statement. If the database reports
 * that zero rows were updated, it is inferred that another transaction
 * has already updated or deleted the row, and the failure of the optimistic
 * lock is reported as a stale-state failure, which may be converted to
 * {@link jakarta.persistence.OptimisticLockException} at an API boundary.
 * <p>
 * An unchanged entity is not automatically validated merely because it was
 * read. Explicit {@link org.hibernate.LockMode#OPTIMISTIC} validation requires
 * a versioned entity; the versionless {@code ALL} and {@code DIRTY} strategies
 * do not provide an equivalent explicit read-validation mode. Their mutation
 * checks depend on previously loaded state, which a merge does not reconstruct
 * from the time a detached object was originally read. Bulk mutations do not
 * automatically perform these per-entity checks.
 * <p>
 * In an inheritance hierarchy, this annotation may only be applied to the
 * root entity, since the optimistic lock checking strategy is inherited
 * by entity subclasses.
 * <p>
 * To exclude a particular attribute from optimistic locking, annotate it as
 * {@link jakarta.persistence.ExcludedFromVersioning @ExcludedFromVersioning}.
 * Then:
 * <ul>
 * <li>changes to that attribute will never trigger a version increment, and
 * <li>the attribute will not be included in the list of fields checked
 *     when {@link OptimisticLockType#ALL} or {@link OptimisticLockType#DIRTY}
 *     is used.
 * </ul>
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.LockMode
 * @see jakarta.persistence.LockModeType
 * @see OptimisticLock
 */
@Target( TYPE )
@Retention( RUNTIME )
public @interface OptimisticLocking {
	/**
	 * The optimistic lock checking strategy.
	 */
	OptimisticLockType type() default OptimisticLockType.VERSION;
}
