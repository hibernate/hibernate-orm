/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.internal;

import javax.persistence.TupleElement;

/**
 * Implementation of the JPA TupleElement contract
 *
 * @author Steve Ebersole
 */
public class TupleElementImpl<E> implements TupleElement<E> {
	private final Class<? extends E> javaType;
	private final String alias;

	public TupleElementImpl(Class<? extends E> javaType, String alias) {
		this.javaType = javaType;
		this.alias = alias;
	}

	@Override
	public Class<? extends E> getJavaType() {
		return javaType;
	}

	@Override
	public String getAlias() {
		return alias;
	}
}
