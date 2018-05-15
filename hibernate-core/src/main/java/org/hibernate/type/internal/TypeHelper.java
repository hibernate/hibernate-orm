/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.StateArrayContributor;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;
import org.hibernate.type.ForeignKeyDirection;
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
			Predicate<StateArrayContributor> copyConditions) {
		deepCopy(
				containerDescriptor,
				source,
				target,
				copyConditions,
				DEEP_COPY_VALUE_PRODUCER
		);
	}

	@SuppressWarnings("unchecked")
	public static void deepCopy(
			ManagedTypeDescriptor<?> containerDescriptor,
			Object[] source,
			Object[] target,
			Predicate<StateArrayContributor> copyConditions,
			BiFunction<StateArrayContributor, Object, Object> targetValueProducer) {
		for ( StateArrayContributor<?> contributor : containerDescriptor.getStateArrayContributors() ) {
			if ( copyConditions.test( contributor ) ) {
				final int position = contributor.getStateArrayPosition();
				target[position] = targetValueProducer.apply( contributor, source[position] );
			}
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
	@SuppressWarnings("unchecked")
	public static Object[] replace(
			EntityTypeDescriptor<?> entityDescriptor,
			Object originalEntity,
			Object targetEntity,
			Map copyCache,
			Object owner,
			SessionImplementor session) {
		// todo (6.0) : better way?

		final Object[] originalValues = entityDescriptor.getPropertyValues( originalEntity );
		final Object[] targetValues = entityDescriptor.getPropertyValues( targetEntity );

		final Object[] copied = new Object[ originalValues.length ];

		for ( StateArrayContributor contributor : entityDescriptor.getStateArrayContributors() ) {
			final int position = contributor.getStateArrayPosition();

			if ( originalValues[ position ] == LazyPropertyInitializer.UNFETCHED_PROPERTY
					|| originalValues[ position ] == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
				copied[ position ] = targetValues[ position ];
			}
			else if ( targetValues[ position ] == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
				// Should be no need to check for target[position] == PropertyAccessStrategyBackRefImpl.UNKNOWN
				// because PropertyAccessStrategyBackRefImpl.get( object ) returns
				// PropertyAccessStrategyBackRefImpl.UNKNOWN, so target[position] == original[position].
				//
				// We know from above that original[position] != LazyPropertyInitializer.UNFETCHED_PROPERTY &&
				// original[position] != PropertyAccessStrategyBackRefImpl.UNKNOWN;
				// This is a case where the entity being merged has a lazy property
				// that has been initialized. Copy the initialized value from original.
				if ( contributor.getMutabilityPlan().isMutable() ) {
					copied[ position ] = contributor.getMutabilityPlan().deepCopy( originalValues[ position ] );
				}
				else {
					copied[ position ] = originalValues[ position ];
				}
			}
			else {
				copied[ position ] = contributor.replace(
						originalValues[ position ],
						targetValues[ position ],
						owner,
						copyCache,
						session
				);
			}
		}
		return copied;
	}

	@SuppressWarnings("unchecked")
	public static Object[] replace(
			EntityTypeDescriptor<?> entityDescriptor,
			Object originalEntity,
			Object targetEntity,
			Map copyCache,
			Object owner,
			ForeignKeyDirection foreignKeyDirection,
			SessionImplementor session) {
		// todo (6.0) : better way?

		final Object[] originalValues = entityDescriptor.getPropertyValues( originalEntity );
		final Object[] targetValues = entityDescriptor.getPropertyValues( targetEntity );

		final Object[] copied = new Object[ originalValues.length ];

		for ( StateArrayContributor contributor : entityDescriptor.getStateArrayContributors() ) {
			final int position = contributor.getStateArrayPosition();

			if ( originalValues[position] == LazyPropertyInitializer.UNFETCHED_PROPERTY
					|| originalValues[position] == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
				copied[position] = targetValues[ position ];
			}
			else if ( targetValues[ position ] == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
				// Should be no need to check for target[position] == PropertyAccessStrategyBackRefImpl.UNKNOWN
				// because PropertyAccessStrategyBackRefImpl.get( object ) returns
				// PropertyAccessStrategyBackRefImpl.UNKNOWN, so target[position] == original[position].
				//
				// We know from above that original[position] != LazyPropertyInitializer.UNFETCHED_PROPERTY &&
				// original[position] != PropertyAccessStrategyBackRefImpl.UNKNOWN;
				// This is a case where the entity being merged has a lazy property
				// that has been initialized. Copy the initialized value from original.
				if ( contributor.getMutabilityPlan().isMutable() ) {
					copied[position] = contributor.getMutabilityPlan().deepCopy( originalValues[ position ] );
				}
				else {
					copied[position] = originalValues[ position ];
				}
			}
			else {
				copied[position] = contributor.replace( originalValues[ position ], targetValues[ position ], owner, copyCache, session );
			}
		}

		return copied;
	}

	public static Object[] replaceAssociations(
			ManagedTypeDescriptor<?> managedTypeDescriptor,
			Object original,
			Object target,
			Map copyCache,
			Object owner,
			ForeignKeyDirection foreignKeyDirection,
			SessionImplementor session) {
		// todo (6.0) : better way?

		final List<StateArrayContributor<?>> contributors = managedTypeDescriptor.getStateArrayContributors();

		final Object[] originalValues = original == null
				? new Object[ contributors.size() ]
				: managedTypeDescriptor.getPropertyValues( original );
		final Object[] targetValues = target == null
				? new Object[ contributors.size() ]
				: managedTypeDescriptor.getPropertyValues( target );

		final Object[] copied = new Object[ originalValues.length ];

		for ( StateArrayContributor contributor : contributors ) {
			final int position = contributor.getStateArrayPosition();

			if ( originalValues[ position ] == LazyPropertyInitializer.UNFETCHED_PROPERTY
					|| originalValues[ position ] == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
				copied[ position ] = targetValues[ position ];
			}
			else if ( contributor instanceof EmbeddedValuedNavigable ) {
				replaceAssociations(
						( (EmbeddedValuedNavigable) contributor ).getEmbeddedDescriptor(),
						originalValues[ position ],
						targetValues[ position ],
						copyCache,
						owner,
						foreignKeyDirection,
						session
				);
				copied[ position ] = targetValues[ position ];
			}
			else if ( contributor instanceof EntityValuedNavigable || contributor instanceof PluralPersistentAttribute ) {
				copied[position] = contributor.replace( originalValues[ position ], targetValues[ position ], owner, copyCache, foreignKeyDirection, session );
			}
			else {
				copied[ position ] = targetValues[ position ];
			}
		}
		return copied;
	}
}
