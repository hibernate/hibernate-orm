/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.internal.selection;

import java.io.Serializable;
import java.util.List;
import javax.persistence.criteria.Selection;

import org.hibernate.query.criteria.spi.JpaCriteriaBuilderImplementor;
import org.hibernate.query.criteria.spi.JpaSelectionImplementor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * The Hibernate implementation of the JPA {@link Selection}
 * contract.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSimpleSelection<X>
		extends AbstractSelection<X>
		implements JpaSelectionImplementor<X>, Serializable {
	public AbstractSimpleSelection(
			JpaCriteriaBuilderImplementor criteriaBuilder,
			JavaTypeDescriptor<X> javaTypeDescriptor) {
		super( criteriaBuilder, javaTypeDescriptor );
	}

	public JpaSelectionImplementor<X> alias(String alias) {
		setAlias( alias );
		return this;
	}

	public boolean isCompoundSelection() {
		return false;
	}

	public List<Selection<?>> getCompoundSelectionItems() {
		throw new IllegalStateException( "Not a compound selection" );
	}
}
