/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.internal.selection;

import java.io.Serializable;
import javax.persistence.criteria.Selection;

import org.hibernate.query.criteria.internal.ParameterContainer;
import org.hibernate.query.criteria.internal.expression.AbstractTupleElement;
import org.hibernate.query.criteria.spi.JpaCriteriaBuilderImplementor;
import org.hibernate.query.criteria.spi.JpaSelectionImplementor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * The Hibernate implementation of the JPA {@link Selection}
 * contract.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSelection<X>
		extends AbstractTupleElement<X>
		implements JpaSelectionImplementor<X>, ParameterContainer, Serializable {
	public AbstractSelection(
			JpaCriteriaBuilderImplementor criteriaBuilder,
			JavaTypeDescriptor<X> javaTypeDescriptor) {
		super( criteriaBuilder, javaTypeDescriptor );
	}

	public Selection<X> alias(String alias) {
		setAlias( alias );
		return this;
	}
}