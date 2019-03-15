/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.internal;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.spi.EntityEntryFactory;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.descriptor.java.spi.EntityMutabilityPlan;

/**
 * @author Steve Ebersole
 */
public class EntityMutabilityPlanImpl implements EntityMutabilityPlan {
	private final EntityEntryFactory entityEntryFactory;
	private final String associatedEntityName;
	private final boolean isMutable;

	public EntityMutabilityPlanImpl(
			EntityEntryFactory entityEntryFactory,
			String associatedEntityName,
			boolean isMutable) {
		this.entityEntryFactory = entityEntryFactory;
		this.associatedEntityName = associatedEntityName;
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

	@Override
	public Object replace(
			Navigable navigable,
			Object originalValue,
			Object targetValue,
			Object owner,
			Map copyCache,
			SessionImplementor session) {
		if ( originalValue == null ) {
			return null;
		}

		Object cached = copyCache.get( originalValue );
		if ( cached != null ) {
			return cached;
		}

		if ( originalValue == targetValue ) {
			return targetValue;
		}

		if ( session.getContextEntityIdentifier( originalValue ) == null &&
				ForeignKeys.isTransient( associatedEntityName, originalValue, Boolean.FALSE, session ) ) {
			// original is transient; it is possible that original is a "managed" entity that has
			// not been made persistent yet, so check if copyCache contains original as a "managed" value
			// that corresponds with some "merge" value.
			if ( copyCache.containsValue( originalValue ) ) {
				return originalValue;
			}
			else {
				// the transient entity is not "managed"; add the merge/managed pair to copyCache
				final Object copy = session.getFactory().getMetamodel()
						.getEntityDescriptor( associatedEntityName )
						.instantiate( null, session );
				copyCache.put( originalValue, copy );
				return copy;
			}
		}
		else {
			assert navigable instanceof EntityTypeDescriptor;

			EntityTypeDescriptor entityDescriptor = (EntityTypeDescriptor) navigable;
			Object id = entityDescriptor.getIdentifier( originalValue );
			if ( id == null ) {
				throw new AssertionFailure(
						"non-transient entity has a null id: " + originalValue.getClass().getName()
				);
			}

			id = entityDescriptor.getIdentifierDescriptor().getJavaTypeDescriptor().getMutabilityPlan().replace(
					entityDescriptor,
					id,
					null,
					owner,
					copyCache,
					session
			);

			// todo (6.0) - this is a hack to get basic stuff working.
			return session.internalLoad(
					entityDescriptor.getEntityName(),
					id,
					true,
					false
			);
		}
	}

	@Override
	public Object replace(
			Navigable navigable,
			Object originalValue,
			Object targetValue,
			Object owner,
			Map copyCache,
			ForeignKeyDirection foreignKeyDirection,
			SessionImplementor session) {
		throw new NotYetImplementedFor6Exception(  );
	}
}
