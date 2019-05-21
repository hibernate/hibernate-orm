/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import javax.persistence.TupleElement;

import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * API extension to the JPA {@link TupleElement} contract
 *
 * @author Steve Ebersole
 */
public interface JpaTupleElement<T> extends TupleElement<T>, JpaCriteriaNode {
	JavaTypeDescriptor<T> getJavaTypeDescriptor();

	@Override
	default Class<? extends T> getJavaType() {
		// todo (6.0) : can this signature just return `Class<T>`?
		return getJavaTypeDescriptor() == null ? null : getJavaTypeDescriptor().getJavaType();
	}
}
