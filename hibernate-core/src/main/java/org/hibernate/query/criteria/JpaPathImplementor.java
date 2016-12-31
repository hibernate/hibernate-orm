/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import javax.persistence.metamodel.Attribute;

import org.hibernate.Incubating;
import org.hibernate.sqm.parser.criteria.tree.path.JpaPath;

/**
 * Hibernate ORM specialization of the JPA {@link javax.persistence.criteria.Path}
 * contract.
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
@Incubating
public interface JpaPathImplementor<X> extends JpaExpressionImplementor<X>, JpaPath<X>, JpaPathSourceImplementor<X> {
	/**
	 * Retrieve reference to the attribute this path represents.
	 *
	 * @return The metamodel attribute.
	 */
	Attribute<?, ?> getAttribute();

	/**
	 * Defines handling for the JPA 2.1 TREAT down-casting feature.
	 *
	 * @param treatAsType The type to treat the path as.
	 * @param <T> The parameterized type representation of treatAsType.
	 *
	 * @return The properly typed view of this path.
	 */
	<T extends X> JpaPathImplementor<T> treatAs(Class<T> treatAsType);
}
