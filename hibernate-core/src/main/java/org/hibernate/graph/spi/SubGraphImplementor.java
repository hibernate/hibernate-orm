/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph.spi;

import javax.persistence.metamodel.Attribute;

import org.hibernate.graph.CannotBecomeEntityGraphException;
import org.hibernate.graph.CannotContainSubGraphException;
import org.hibernate.graph.SubGraph;
import org.hibernate.metamodel.model.domain.spi.PersistentAttributeDescriptor;

/**
 * Integration version of the SubGraph contract
 *
 * @author Steve Ebersole
 */
public interface SubGraphImplementor<J> extends SubGraph<J>, GraphImplementor<J> {
	@Override
	SubGraphImplementor<J> makeCopy(boolean mutable);

	@Override
	default SubGraphImplementor<J> makeSubGraph(boolean mutable) {
		if ( ! mutable && ! isMutable() ) {
			return this;
		}

		return makeCopy( mutable );
	}

	@Override
	RootGraphImplementor<J> makeRootGraph(String name, boolean mutable) throws CannotBecomeEntityGraphException;

	@Override
	<AJ> SubGraphImplementor<AJ> addKeySubGraph(String attributeName);

	@Override
	<AJ> AttributeNodeImplementor<AJ> addAttributeNode(Attribute<? extends J, AJ> attribute);

	@Override
	default <AJ> SubGraphImplementor<? extends AJ> addKeySubGraph(
			PersistentAttributeDescriptor<? extends J, AJ> attribute, Class<? extends AJ> subType)
			throws CannotContainSubGraphException {
		return null;
	}
}
