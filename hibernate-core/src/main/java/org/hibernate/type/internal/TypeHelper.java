/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import java.io.Serializable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.StateArrayElementContributor;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;

/**
 * @author Steve Ebersole
 */
public class TypeHelper {
	public static final BiFunction<StateArrayElementContributor,Object,Object> DEEP_COPY_VALUE_PRODUCER = new BiFunction<StateArrayElementContributor, Object, Object>() {
		@Override
		public Object apply(StateArrayElementContributor navigable, Object sourceValue) {
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
			Predicate<StateArrayElementContributor> skipCondition) {
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
			ManagedTypeDescriptor containerDescriptor,
			Object[] source,
			Object[] target,
			Predicate<StateArrayElementContributor> skipCondition,
			BiFunction<StateArrayElementContributor,Object,Object> targetValueProducer) {
		containerDescriptor.visitStateArrayNavigables(
				new Consumer<StateArrayElementContributor>() {
					private int i = -1;

					@Override
					public void accept(StateArrayElementContributor navigable) {
						i++;

						if ( skipCondition.test( navigable ) ) {
							return;
						}

						target[i] = targetValueProducer.apply( navigable, source[i] );
					}
				}
		);
	}

	public static String toLoggableString(Object[] state, ManagedTypeDescriptor<?> managedTypeDescriptor) {
		final StringBuilder buffer = new StringBuilder();
		buffer.append( managedTypeDescriptor.getNavigableName() )
				.append( '[' );

		managedTypeDescriptor.visitAttributes(
				new Consumer<PersistentAttribute>() {
					int i = 0;

					@Override
					public void accept(PersistentAttribute attribute) {
						if ( i > 0 ) {
							buffer.append( ", " );
						}

						buffer.append( attribute.getJavaTypeDescriptor().toString( state[i] ) );
						i++;
					}
				}
		);

		return buffer.append( ']' ).toString();
	}

	public static Serializable[] disassemble(final Object[] state, final  boolean[] nonCacheable, ManagedTypeDescriptor descriptor) {
		final Serializable[] disassembledState = new Serializable[state.length];
		descriptor.visitStateArrayNavigables( new Consumer<StateArrayValuedNavigable>() {
			int position = 0;

			@Override
			public void accept(StateArrayValuedNavigable attribute) {
				if ( nonCacheable != null && nonCacheable[position] ) {
					disassembledState[position] = LazyPropertyInitializer.UNFETCHED_PROPERTY;
				}
				else if ( state[position] == LazyPropertyInitializer.UNFETCHED_PROPERTY || state[position] == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
					disassembledState[position] = (Serializable) state[position];
				}
				else {
					disassembledState[position] = attribute.getJavaTypeDescriptor()
							.getMutabilityPlan()
							.disassemble( state[position] );
				}
				position++;
			}
		} );
		return disassembledState;
	}

	public static Object[] assemble(final Serializable[] disassembledState, ManagedTypeDescriptor descriptor) {
		final Object[] assembledProps = new Object[disassembledState.length];
		descriptor.visitStateArrayNavigables( new Consumer<StateArrayValuedNavigable>() {
			int position = 0;

			@Override
			public void accept(StateArrayValuedNavigable attribute) {
				if ( disassembledState[position] == LazyPropertyInitializer.UNFETCHED_PROPERTY || disassembledState[position] == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
					assembledProps[position] = disassembledState[position];
				}
				else {
					assembledProps[position] = attribute.getJavaTypeDescriptor().getMutabilityPlan().assemble(
							disassembledState[position] );
				}
				position++;
			}
		} );

		return assembledProps;
	}
}
