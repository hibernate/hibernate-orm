/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import org.hibernate.query.criteria.internal.ValueHandlerFactory;
import org.hibernate.sqm.parser.criteria.tree.JpaTupleElement;

/**
 * Hibernate ORM specialization of the JPA {@link javax.persistence.TupleElement}
 * contract.
 *
 * @author Steve Ebersole
 */
public interface JpaTupleElementImplementor<X> extends JpaTupleElement<X> {
	//ValueHandlerFactory.ValueHandler<X> getValueHandler();
}
