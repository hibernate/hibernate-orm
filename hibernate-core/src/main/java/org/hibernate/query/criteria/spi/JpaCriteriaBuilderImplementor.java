/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.List;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Selection;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.NonHibernaeNodeException;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public interface JpaCriteriaBuilderImplementor extends HibernateCriteriaBuilder {
	@Override
	SessionFactoryImplementor getSessionFactory();

	default <X> JavaTypeDescriptor<X> resolveJavaTypeDescriptor(Class<X> javaType) {
		return getSessionFactory().getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( javaType );
	}

	/**
	 * Centralize checking of criteria query multi-selects as defined by the
	 * {@link CriteriaQuery#multiselect(List)}  method.
	 *
	 * @param selections The selection varargs to check
	 *
	 * @throws IllegalArgumentException If the selection items are not valid per {@link CriteriaQuery#multiselect}
	 * documentation.
	 * <i>&quot;An argument to the multiselect method must not be a tuple-
	 * or array-valued compound selection item.&quot;</i>
	 */
	void checkMultiSelect(List<Selection<?>> selections);


	// todo (6.0) : rather than `#checkXYZ`, I think a better option is a cast operation with built-in checking

	/**
	 * Given a straight-up JPA Selection reference, convert that to a
	 * Hibernate JpaSelectionImplementor validating that it is in
	 * fact a JpaSelectionImplementor and throwing an exception if not
	 */
	default <X> JpaSelectionImplementor<X> asHibernateSelection(Selection<X> selection) {
		if ( !JpaSelectionImplementor.class.isInstance( selection ) ) {
			throw new NonHibernaeNodeException(
					"Expecting javax.persistence.criteria.Selection to be " +
							"a Hibernate implementation, but found " +
							selection.toString()
			);
		}
		return (JpaSelectionImplementor<X>) selection;
	}

	default <X> JpaExpressionImplementor<X> asHibernateExpression(Expression<X> expression) {
		if ( !JpaExpressionImplementor.class.isInstance( expression ) ) {
			throw new NonHibernaeNodeException(
					"Expecting javax.persistence.criteria.Expression to be " +
							"a Hibernate implementation, but found " +
							expression.toString()
			);
		}
		return (JpaExpressionImplementor<X>) expression;
	}

	default JpaPredicateImplementor asHibernatePredicate(Predicate predicate) {
		if ( !JpaPredicateImplementor.class.isInstance( predicate ) ) {
			throw new NonHibernaeNodeException(
					"Expecting javax.persistence.criteria.Predicate to be " +
							"a Hibernate implementation,, but found " +
							predicate.toString()
			);
		}

		return (JpaPredicateImplementor) predicate;
	}

	JpaPredicateImplementor wrap(Expression<Boolean> expression);
}
