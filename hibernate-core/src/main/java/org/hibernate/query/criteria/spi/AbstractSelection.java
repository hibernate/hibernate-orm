/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.List;

import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Base support for {@link JpaSelection} impls
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbstractSelection<X>
		extends AbstractTupleElement<X>
		implements SelectionImplementor<X> {
	protected AbstractSelection(Class<X> javaType, CriteriaNodeBuilder criteriaBuilder) {
		super( javaType, criteriaBuilder );
	}

	protected AbstractSelection(JavaTypeDescriptor<X> javaTypeDescriptor, CriteriaNodeBuilder criteriaBuilder) {
		super( javaTypeDescriptor, criteriaBuilder );
	}

	@Override
	public JpaSelection<X> alias(String alias) {
		setAlias( alias );
		return this;
	}

	@Override
	public boolean isCompoundSelection() {
		return false;
	}

	@Override
	public List<? extends JpaSelection<?>> getSelectionItems() {
		throw new IllegalStateException( "Not a compound selection" );
	}
}
