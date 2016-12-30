/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.internal.expression;

import java.io.Serializable;
import javax.persistence.TupleElement;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.internal.AbstractNode;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public abstract class AbstractTupleElement<X>
		extends AbstractNode
		implements TupleElement<X>, Serializable {
	private final Class originalJavaType;
	private Class<X> javaType;
	private String alias;

	protected AbstractTupleElement(HibernateCriteriaBuilder criteriaBuilder, Class<X> javaType) {
		super( criteriaBuilder );
		this.originalJavaType = javaType;
		this.javaType = javaType;
	}

	@Override
	public Class<X> getJavaType() {
		return javaType;
	}

	@SuppressWarnings({ "unchecked" })
	protected void resetJavaType(Class targetType) {
		this.javaType = targetType;
	}

	@Override
	public String getAlias() {
		return alias;
	}

	/**
	 * Protected access to define the alias.
	 *
	 * @param alias The alias to use.
	 */
	protected void setAlias(String alias) {
		this.alias = alias;
	}
}
