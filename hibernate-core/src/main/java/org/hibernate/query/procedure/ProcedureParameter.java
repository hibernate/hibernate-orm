/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.procedure;

import javax.persistence.ParameterMode;

import org.hibernate.Incubating;
import org.hibernate.query.QueryParameter;

/**
 * NOTE: Consider this contract (and its sub-contracts) as incubating as we transition to 6.0 and SQM
 *
 * @author Steve Ebersole
 */
@Incubating
public interface ProcedureParameter<T> extends QueryParameter<T> {
	/**
	 * Retrieves the parameter "mode".  Only really pertinent in regards to procedure/function calls.
	 * In all other cases the mode would be {@link ParameterMode#IN}
	 *
	 * @return The parameter mode.
	 */
	ParameterMode getMode();

	/**
	 * How will an unbound value be handled in terms of the JDBC parameter?
	 *
	 * @return {@code true} here indicates that NULL should be passed; {@code false} indicates
	 * that it is ignored.
	 *
	 * @deprecated (since 6.0) : Passing null or not is now triggered by whether
	 * setting the parameter was called at all.  In other words a distinction is
	 * made between calling `setParameter` passing {@code null} versus not calling
	 * `setParameter` at all.  In the first case, we pass along the {@code null}; in
	 * the second we do not pass {@code null}.
	 */
	@Deprecated
	default boolean isPassNullsEnabled() {
		return false;
	}

	/**
	 * Controls how unbound values for this IN/INOUT parameter registration will be handled prior to
	 * execution.
	 *
	 * @param enabled {@code true} indicates that the NULL should be passed; {@code false} indicates it should not.
	 *
	 * @deprecated (since 6.0) : see {@link #isPassNullsEnabled}
	 */
	@Deprecated
	default void enablePassingNulls(boolean enabled) {
	}
}
