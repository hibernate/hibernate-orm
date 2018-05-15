/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.io.Serializable;

import org.hibernate.type.descriptor.java.MutabilityPlan;

/**
 * @apiNote Created a specialized subclass because I am pretty sure we will end
 * up adding some collection-specific mutability-based functionality (copy for
 * merge, insertable elements, etc).
 *
 * @author Steve Ebersole
 */
public interface CollectionMutabilityPlan<C> extends MutabilityPlan<C> {
	CollectionMutabilityPlan INSTANCE = new CollectionMutabilityPlan() {
		@Override
		public boolean isMutable() {
			return true;
		}

		@Override
		public Object deepCopy(Object value) {
			return value;
		}

		@Override
		public Serializable disassemble(Object value) {
			return (Serializable) value;
		}

		@Override
		public Object assemble(Serializable cached) {
			return cached;
		}
	};
}
