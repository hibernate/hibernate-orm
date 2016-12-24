/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query;

import javax.persistence.ParameterMode;

import org.hibernate.Incubating;
import org.hibernate.procedure.spi.ParameterRegistrationImplementor;
import org.hibernate.type.spi.Type;

/**
 * Represents a parameter defined in the source (HQL/JPQL or criteria) query.
 *
 * NOTE: Consider this contract (and its sub-contracts) as incubating as we transition to 6.0 and SQM
 *
 * @author Steve Ebersole
 */
@Incubating
public interface QueryParameter<T> extends javax.persistence.Parameter<T> {
	/**
	 * Is this a {@code IN}, {@code OUT} or {@code INOUT} parameter.
	 * <p/>
	 * Only really pertinent in regards to procedure/function calls.  In all
	 * other cases the mode would be {@link ParameterMode#IN}
	 *
	 * @return The parameter mode.
	 */
	default ParameterMode getMode() {
		return ParameterMode.IN;
	}

	/**
	 * Does this parameter allow multi-valued (collection, array, etc) binding?
	 * <p/>
	 * This is only valid for HQL/JPQL and (I think) Criteria queries, and is
	 * determined based on the context of the parameters declaration.
	 *
	 * @return {@code true} indicates that multi-valued binding is allowed for this
	 * parameter
	 */
	boolean allowsMultiValuedBinding();

	/**
	 * How will an unbound value be handled in terms of the JDBC parameter?
	 *
	 * @return {@code true} here indicates that NULL should be passed; {@code false} indicates
	 * that it is ignored.
	 *
	 * @see ParameterRegistrationImplementor#isPassNullsEnabled()
	 */
	boolean isPassNullsEnabled();

	/**
	 * Get the Hibernate Type associated with this parameter, if one.  May
	 * return {@code null}.
	 *
	 * @return The associated Hibernate Type, may be {@code null}.
	 */
	Type getHibernateType();

	/**
	 * JPA has a different definition of positional parameters than what legacy Hibernate HQL had.  In JPA,
	 * the parameter holders are labelled (named :/).  At any rate the semantics are different and we often
	 * need to understand which we are dealing with (and applications might too).
	 *
	 * @return {@code true} if this is a JPA-style positional parameter; {@code false} would indicate
	 * we have either a named parameter ({@link #getName()} would return a non-{@code null} value) or a native
	 * Hibernate positional parameter.
	 *
	 * @deprecated (since 6.0) support for legacy positional parameters has been dropped, so
	 * by design any positional parameter is now a "jpa positional parameter".
	 */
	@Deprecated
	default boolean isJpaPositionalParameter() {
		// per discussion above, any positional parameter is a "jpa positional parameter"
		return getPosition() != null;
	}

}
