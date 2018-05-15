/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.internal;

import java.io.Serializable;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.EntityEntryFactory;
import org.hibernate.type.descriptor.java.spi.EntityMutabilityPlan;

/**
 * @author Steve Ebersole
 */
public class EntityMutabilityPlanImpl implements EntityMutabilityPlan {
	private final EntityEntryFactory entityEntryFactory;
	private final boolean isMutable;

	public EntityMutabilityPlanImpl(
			EntityEntryFactory entityEntryFactory,
			boolean isMutable) {
		this.entityEntryFactory = entityEntryFactory;
		this.isMutable = isMutable;
	}

	@Override
	public EntityEntryFactory getEntityEntryFactory() {
		return entityEntryFactory;
	}

	@Override
	public boolean isMutable() {
		return isMutable;
	}

	@Override
	public Object deepCopy(Object value) {
		return value;
	}

	@Override
	public Serializable disassemble(Object value) {
		// todo (6.0) : this requires some capability to ask the EntityIdentifier to extract the id value from an entity instance.
		//		however, I'm not sure deepCopy, disassemble and assemble make sense here
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public Object assemble(Serializable cached) {
		throw new NotYetImplementedFor6Exception();
	}
}
