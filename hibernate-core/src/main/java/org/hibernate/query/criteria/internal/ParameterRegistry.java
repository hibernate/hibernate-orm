/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal;
import javax.persistence.criteria.ParameterExpression;

/**
 * A registry for parameters.  In criteria queries, parameters must be actively seeked out as expressions and predicates
 * are added to the {@link org.hibernate.criterion.CriteriaQuery}; this contract allows the various subcomponents to
 * register any parameters they contain.
 *
 * @author Steve Ebersole
 */
public interface ParameterRegistry {
	/**
	 * Registers the given parameter with this regitry.
	 *
	 * @param parameter The parameter to register.
	 */
	public void registerParameter(ParameterExpression<?> parameter);
}
