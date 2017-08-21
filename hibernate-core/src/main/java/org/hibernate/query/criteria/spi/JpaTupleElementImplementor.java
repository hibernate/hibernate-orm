/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import javax.persistence.TupleElement;

import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Hibernate ORM specialization of the JPA {@link TupleElement}
 * contract.
 *
 * @author Steve Ebersole
 */
public interface JpaTupleElementImplementor<X> extends TupleElement<X>, JpaCriteriaNode {
	// todo (6.0) : need something like 5.x's `ValueHandlerFactory.ValueHandler`
	//		or can that be limited to just literals
	//
	//		also, often the choice of how to handle a literal (JDBC parameter, SQL literal)
	//		is usage-specific (see `org.hibernate.query.QueryLiteralRendering`) - and
	//		given the fact we have this enum `ValueHandler` may not really even be needed.
	//		And even if it is needed, seems to me that JavaTypeDescriptor might be the
	//		better place to expose such a thing rather than essentially a case-statement
	//		over the literal type.

	// ValueHandlerFactory.ValueHandler<X> getValueHandler();

	JavaTypeDescriptor<X> getJavaTypeDescriptor();

	@Override
	default Class<? extends X> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}
}
