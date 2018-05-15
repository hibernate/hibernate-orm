/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query;

import javax.persistence.ParameterMode;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;

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
	 * Get the Hibernate Type associated with this parameter, if one.  May
	 * return {@code null}.
	 *
	 * @return The associated Hibernate Type, may be {@code null}.
	 */
	AllowableParameterType<T> getHibernateType();
}
