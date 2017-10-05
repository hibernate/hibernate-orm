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
import org.hibernate.metamodel.model.domain.spi.StateArrayElementContributor;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;

import static java.util.stream.Collectors.joining;

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
			ManagedTypeDescriptor<?> containerDescriptor,
			Object[] source,
			Object[] target,
			Predicate<StateArrayElementContributor> skipCondition,
			BiFunction<StateArrayElementContributor,Object,Object> targetValueProducer) {

		// get a stream of all Navigables
		containerDescriptor.navigableStream( StateArrayElementContributor.class )
				.forEach(
						attribute -> {
							if ( skipCondition.test( attribute ) ) {
								return;
							}

							final int position = attribute.getStateArrayPosition();
							target[position] = targetValueProducer.apply( attribute, source[position] );
						}
				);
	}

	@SuppressWarnings("unchecked")
	public static String toLoggableString(Object[] state, ManagedTypeDescriptor<?> managedTypeDescriptor) {
		return managedTypeDescriptor.navigableStream( StateArrayElementContributor.class )
				.map( attribute -> attribute.getJavaTypeDescriptor().toString( state[attribute.getStateArrayPosition()] ) )
				.collect( joining( ", ", managedTypeDescriptor.getNavigableName() + '[', "]" ) );
	}

	@SuppressWarnings("unchecked")
	public static Serializable[] disassemble(final Object[] state, final  boolean[] nonCacheable, ManagedTypeDescriptor<?> descriptor) {
		final Serializable[] disassembledState = new Serializable[state.length];

		descriptor.navigableStream( StateArrayElementContributor.class )
				.forEach(
						attribute -> {
							final int position = attribute.getStateArrayPosition();
							if ( nonCacheable != null && nonCacheable[position] ) {
								disassembledState[position] = LazyPropertyInitializer.UNFETCHED_PROPERTY;
							}
							else if ( state[position] == LazyPropertyInitializer.UNFETCHED_PROPERTY
									|| state[position] == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
								disassembledState[position] = (Serializable) state[position];
							}
							else {
								disassembledState[position] = attribute.getMutabilityPlan().disassemble( state[position] );
							}
						}
				);

		return disassembledState;
	}

	public static Object[] assemble(final Serializable[] disassembledState, ManagedTypeDescriptor<?> descriptor) {
		final Object[] assembledProps = new Object[disassembledState.length];

		descriptor.navigableStream( StateArrayElementContributor.class )
				.forEach(
						attribute -> {
							final int position = attribute.getStateArrayPosition();
							if ( disassembledState[position] == LazyPropertyInitializer.UNFETCHED_PROPERTY
									|| disassembledState[position] == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
								assembledProps[position] = disassembledState[position];
							}
							else {
								assembledProps[position] = attribute.getJavaTypeDescriptor().getMutabilityPlan().assemble(
										disassembledState[position] );
							}
						}
				);

		return assembledProps;
	}
}
