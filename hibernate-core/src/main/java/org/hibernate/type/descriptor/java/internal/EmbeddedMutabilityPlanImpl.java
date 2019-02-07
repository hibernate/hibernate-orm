/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.EmbeddedMutabilityPlan;
import org.hibernate.type.internal.TypeHelper;

/**
 * @author Chris Cranford
 */
public class EmbeddedMutabilityPlanImpl implements EmbeddedMutabilityPlan {
	private final EmbeddedTypeDescriptor embeddedTypeDescriptor;

	public EmbeddedMutabilityPlanImpl(EmbeddedTypeDescriptor embeddedTypeDescriptor) {
		this.embeddedTypeDescriptor = embeddedTypeDescriptor;
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object deepCopy(Object value) {
		final Object[] values = embeddedTypeDescriptor.getPropertyValues( value );
		embeddedTypeDescriptor.visitStateArrayContributors(
				contributor -> {
					final int index = contributor.getStateArrayPosition();
					final MutabilityPlan mutabilityPlan = contributor.getMutabilityPlan();
					values[ index ] = mutabilityPlan.deepCopy( values[ index ] );
				}
		);

		Object instance = embeddedTypeDescriptor.instantiate( null );
		embeddedTypeDescriptor.setPropertyValues( instance, values );

		return instance;
	}

	@Override
	public Serializable disassemble(Object value) {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public Object assemble(Serializable cached) {
		throw new NotYetImplementedFor6Exception(  );
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

		final Object result = targetValue == null
				? embeddedTypeDescriptor.instantiate( session )
				: targetValue;

		Object[] values = TypeHelper.replace(
				embeddedTypeDescriptor,
				originalValue,
				targetValue,
				owner,
				copyCache,
				session
		);

		embeddedTypeDescriptor.setPropertyValues( result, values );

		return result;
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
		if ( originalValue == null ) {
			return null;
		}

		final Object result = targetValue == null
				? embeddedTypeDescriptor.instantiate( session )
				: targetValue;

		Object[] values = TypeHelper.replace(
				embeddedTypeDescriptor,
				originalValue,
				targetValue,
				owner,
				copyCache,
				foreignKeyDirection,
				session
		);

		embeddedTypeDescriptor.setPropertyValues( result, values );

		return result;
	}
}
