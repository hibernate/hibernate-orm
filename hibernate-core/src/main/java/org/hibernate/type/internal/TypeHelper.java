/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import java.io.Serializable;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.StateArrayContributor;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class TypeHelper {
	public static final BiFunction<StateArrayContributor,Object,Object> DEEP_COPY_VALUE_PRODUCER = new BiFunction<StateArrayContributor, Object, Object>() {
		@Override
		public Object apply(StateArrayContributor navigable, Object sourceValue) {
			if ( sourceValue == LazyPropertyInitializer.UNFETCHED_PROPERTY
					|| sourceValue == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
				return sourceValue;
			}
			else {
				return navigable.getMutabilityPlan().deepCopy( sourceValue );
			}
		}
	};

	@SuppressWarnings("unchecked")
	public static void deepCopy(
			ManagedTypeDescriptor containerDescriptor,
			Object[] source,
			Object[] target,
			Predicate<StateArrayContributor> skipCondition) {
		deepCopy(
				containerDescriptor,
				source,
				target,
				skipCondition,
				DEEP_COPY_VALUE_PRODUCER
		);
	}

	@SuppressWarnings("unchecked")
	public static void deepCopy(
			ManagedTypeDescriptor<?> containerDescriptor,
			Object[] source,
			Object[] target,
			Predicate<StateArrayContributor> skipCondition,
			BiFunction<StateArrayContributor,Object,Object> targetValueProducer) {
		for ( StateArrayContributor<?> contributor : containerDescriptor.getStateArrayContributors() ) {
			if ( skipCondition.test( contributor ) ) {
				return;
			}

			final int position = contributor.getStateArrayPosition();
			target[position] = targetValueProducer.apply( contributor, source[position] );
		}
	}

	@SuppressWarnings("unchecked")
	public static String toLoggableString(Object[] state, ManagedTypeDescriptor<?> managedTypeDescriptor) {
		final StringBuilder buffer = new StringBuilder( managedTypeDescriptor.getNavigableName() + '[' );

		boolean firstTime = true;

		for ( StateArrayContributor<?> contributor : managedTypeDescriptor.getStateArrayContributors() ) {
			if ( firstTime ) {
				firstTime = false;
			}
			else {
				buffer.append( ", " );
			}

			buffer.append(
					( (JavaTypeDescriptor) contributor.getJavaTypeDescriptor() ).toString(
							state[ contributor.getStateArrayPosition() ]
					)
			);
		}

		return buffer.append( ']' ).toString();
	}

	@SuppressWarnings("unchecked")
	public static Serializable[] disassemble(
			final Object[] state,
			final boolean[] nonCacheable,
			ManagedTypeDescriptor<?> containerDescriptor) {
		final Serializable[] disassembledState = new Serializable[state.length];

		for ( StateArrayContributor<?> contributor : containerDescriptor.getStateArrayContributors() ) {
			final int position = contributor.getStateArrayPosition();
			if ( nonCacheable != null && nonCacheable[position] ) {
				disassembledState[position] = LazyPropertyInitializer.UNFETCHED_PROPERTY;
			}
			else if ( state[position] == LazyPropertyInitializer.UNFETCHED_PROPERTY
					|| state[position] == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
				disassembledState[position] = (Serializable) state[position];
			}
			else {
				disassembledState[position] = ( (MutabilityPlan) contributor.getMutabilityPlan() ).disassemble( state[position] );
			}
		}

		return disassembledState;
	}

	public static Object[] assemble(final Serializable[] disassembledState, ManagedTypeDescriptor<?> containerDescriptor) {
		final Object[] assembledProps = new Object[disassembledState.length];

		for ( StateArrayContributor<?> contributor : containerDescriptor.getStateArrayContributors() ) {
			final int position = contributor.getStateArrayPosition();

			if ( disassembledState[position] == LazyPropertyInitializer.UNFETCHED_PROPERTY
					|| disassembledState[position] == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
				assembledProps[position] = disassembledState[position];
			}
			else {
				assembledProps[position] = contributor.getJavaTypeDescriptor().getMutabilityPlan().assemble(
						disassembledState[position]
				);
			}
		}

		return assembledProps;
	}
}
