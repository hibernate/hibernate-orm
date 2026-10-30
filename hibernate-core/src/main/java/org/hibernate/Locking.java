package org.hibernate;

import jakarta.persistence.FindOption;
import jakarta.persistence.LockOption;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.RefreshOption;

import java.util.Locale;

/**
 * Support for various aspects of pessimistic locking.
 *
 * @see LockMode#PESSIMISTIC_READ
 * @see LockMode#PESSIMISTIC_WRITE
 * @see LockMode#PESSIMISTIC_FORCE_INCREMENT
 *
 * @author Steve Ebersole
 */
@Incubating(since = "7.1")
public interface Locking {

	/**
	 * In certain circumstances, Hibernate may need to acquire locks
	 * through the use of additional queries.  For example, some
	 * databases may not allow locking rows for queries with a join or
	 * with pagination. This option controls whether Hibernate may select
	 * entities first and acquire their locks with additional statements.
	 * <p>
	 * Selection and acquisition are separate phases. Concurrent changes
	 * may occur between them; acquisition does not rerun the original
	 * predicate or establish unchanged page membership. Existing managed
	 * state is not generally refreshed. A required row disappearing causes
	 * a stale-state failure, rather than successful acquisition.
	 * <p>
	 * Follow-on acquisition requires entity results covering every requested
	 * entity lock target. Scalar attributes, identifiers, scalar-only DTOs,
	 * and scalar aggregate results do not provide this coverage. Mixed results
	 * and DTOs containing entities may provide it. Skip-locked requests are
	 * incompatible with follow-on acquisition. Hibernate rejects these known
	 * incompatible combinations before executing the selection SQL.
	 * <p>
	 * Locking a graph root through this approach does not establish that
	 * protected graph state was read after root acquisition. Applications
	 * coordinating graph access must establish that ordering themselves.
	 *
	 * @see org.hibernate.query.SelectionQuery#setFollowOnStrategy(FollowOn)
	 */
	enum FollowOn implements FindOption, LockOption, RefreshOption {
		/**
		 * Allow follow-on locking when necessary and supported for the result
		 * mapping and requested options.
		 */
		ALLOW,

		/**
		 * Disallow follow-on locking, throwing an exception instead.
		 * This does not make an unsupported direct locking clause valid.
		 */
		DISALLOW,

		/**
		 * Allow execution without the expected locks when follow-on
		 * acquisition would be required.
		 *
		 * @apiNote This can lead to rows not being locked
		 * when they are expected to be.
		 *
		 * @see #DISALLOW
		 */
		IGNORE,

		/**
		 * Request follow-on locking, subject to the applicable execution path,
		 * result mapping, and dialect support.
		 *
		 * @apiNote Incompatible requests may be rejected by Hibernate or the database.
		 */
		FORCE;

		public static FollowOn interpret(String name) {
			if ( name == null ) {
				return null;
			}

			return valueOf( name.toUpperCase( Locale.ROOT ) );
		}

		public static FollowOn fromHint(Object value) {
			if ( value == null ) {
				return Locking.FollowOn.ALLOW;
			}
			else if ( value instanceof Locking.FollowOn strategyValue ) {
				return strategyValue;
			}
			else {
				return Locking.FollowOn.valueOf( value.toString() );
			}
		}

		/**
		 * Interprets the follow-on strategy into the legacy boolean values.
		 *
		 * @return {@code true} if {@linkplain #FORCE}; {@code false} if {@linkplain #DISALLOW};
		 * {@code null} otherwise.
		 *
		 * @see #fromLegacyValue
		 */
		public Boolean asLegacyValue() {
			return switch ( this ) {
				case FORCE -> true;
				case DISALLOW -> false;
				default -> null;
			};
		}

		/**
		 * Given a legacy boolean value, interpret the follow-on strategy.
		 *
		 * @return {@linkplain #FORCE} if {@code true};
		 * {@linkplain #DISALLOW} if {@code false};
		 * {@linkplain #ALLOW} otherwise.
		 *
		 * @see #asLegacyValue()
		 */
		public static FollowOn fromLegacyValue(Boolean value) {
			if ( value == Boolean.TRUE ) {
				return FORCE;
			}
			if ( value == Boolean.FALSE ) {
				return DISALLOW;
			}
			return ALLOW;
		}
	}

	static PessimisticLockScope scopeFromHint(Object value) {
		if ( value == null ) {
			return PessimisticLockScope.NORMAL;
		}
		else if ( value instanceof PessimisticLockScope ref ) {
			return ref;
		}
		else {
			var name = value.toString().toUpperCase( Locale.ROOT );
			return PessimisticLockScope.valueOf( name );
		}
	}
}
