/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Map;

import org.hibernate.Internal;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;

/**
 * Certain operations for working with arrays of property values.
 *
 * @author Steve Ebersole
 */
@Internal
public class TypeHelper {
	/**
	 * Disallow instantiation
	 */
	private TypeHelper() {
	}

	/**
	 * Deep copy a series of values from one array to another...
	 *
	 * @param values The values to copy (the source)
	 * @param types The value types
	 * @param copy an array indicating which values to include in the copy
	 * @param target The array into which to copy the values
	 * @param session The originating session
	 */
	public static void deepCopy(
			final Object[] values,
			final Type[] types,
			final boolean[] copy,
			final Object[] target,
			final SharedSessionContractImplementor session) {
		for ( int i = 0; i < types.length; i++ ) {
			if ( copy[i] ) {
				if ( values[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY
					|| values[i] == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
					target[i] = values[i];
				}
				else {
					target[i] = types[i].deepCopy( values[i], session
						.getFactory() );
				}
			}
		}
	}

	/**
	 * Apply the {@link Type#replace} operation across a series of values.
	 *
	 * @param original The source of the state
	 * @param target The target into which to replace the source values.
	 * @param types The value types
	 * @param session The originating session
	 * @param owner The entity "owning" the values
	 * @param copyCache A map representing a cache of already replaced state
	 *
	 * @return The replaced state
	 */
	public static Object[] replace(
			final Object[] original,
			final Object[] target,
			final Type[] types,
			final SharedSessionContractImplementor session,
			final Object owner,
			final Map<Object, Object> copyCache) {
		Object[] copied = new Object[original.length];
		for ( int i = 0; i < types.length; i++ ) {
			if ( original[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY
					|| original[i] == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
				copied[i] = target[i];
			}
			else if ( target[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
				copied[i] = types[i].replace( original[i], null, session, owner, copyCache );
			}
			else {
				copied[i] = types[i].replace( original[i], target[i], session, owner, copyCache );
			}
		}
		return copied;
	}

	/**
	 * Apply the {@link Type#replace} operation across a series of values.
	 *
	 * @param persister The EntityPersister
	 * @param entity The source of the state
	 * @param session The originating session
	 * @param owner The entity "owning" the values
	 * @param copyCache A map representing a cache of already replaced state
	 *
	 */
	public static void replace(
			final EntityPersister persister,
			final Object entity,
			final SharedSessionContractImplementor session,
			final Object owner,
			final Map<Object, Object> copyCache) {
		final Object[] values = persister.getValues( entity );
		final Type[] types = persister.getPropertyTypes();
		for ( int i = 0; i < types.length; i++ ) {
			final Object oldValue = values[i];
			final Object newValue;
			if ( oldValue != LazyPropertyInitializer.UNFETCHED_PROPERTY
					&& oldValue != PropertyAccessStrategyBackRefImpl.UNKNOWN
					&& ( newValue = types[i].replace( values[i], values[i], session, owner, copyCache ) ) != oldValue ) {
				persister.setValue( entity, i, newValue );
			}
		}
	}

	/**
	 * Apply the {@link Type#replace} operation across a series of values.
	 *
	 * @param original The source of the state
	 * @param target The target into which to replace the source values.
	 * @param types The value types
	 * @param session The originating session
	 * @param owner The entity "owning" the values
	 * @param copyCache A map representing a cache of already replaced state
	 * @param foreignKeyDirection FK directionality to be applied to the replacement
	 *
	 * @return The replaced state
	 */
	public static Object[] replace(
			final Object[] original,
			final Object[] target,
			final Type[] types,
			final SharedSessionContractImplementor session,
			final Object owner,
			final Map<Object, Object> copyCache,
			final ForeignKeyDirection foreignKeyDirection) {
		Object[] copied = new Object[original.length];
		for ( int i = 0; i < types.length; i++ ) {
			if ( original[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY
					|| original[i] == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
				copied[i] = target[i];
			}
			else if ( target[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
				copied[i] = types[i].replace( original[i], null, session, owner, copyCache, foreignKeyDirection );
			}
			else {
				copied[i] = types[i].replace( original[i], target[i], session, owner, copyCache, foreignKeyDirection );
			}
		}
		return copied;
	}

	/**
	 * Apply the {@link Type#replace} operation across a series of values, as long as the corresponding
	 * {@link Type} is an association.
	 * <p>
	 * If the corresponding type is a component type, then apply {@link Type#replace} across the component
	 * subtypes but do not replace the component value itself.
	 *
	 * @param original The source of the state
	 * @param target The target into which to replace the source values.
	 * @param types The value types
	 * @param session The originating session
	 * @param owner The entity "owning" the values
	 * @param copyCache A map representing a cache of already replaced state
	 * @param foreignKeyDirection FK directionality to be applied to the replacement
	 *
	 * @return The replaced state
	 */
	public static Object[] replaceAssociations(
			final Object[] original,
			final Object[] target,
			final Type[] types,
			final SharedSessionContractImplementor session,
			final Object owner,
			final Map<Object, Object> copyCache,
			final ForeignKeyDirection foreignKeyDirection) {
		final Object[] copied = new Object[original.length];
		for ( int i = 0; i < types.length; i++ ) {
			final Object currentOriginal = original[i];
			if ( currentOriginal == LazyPropertyInitializer.UNFETCHED_PROPERTY
					|| currentOriginal == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
				copied[i] = target[i];
			}
			else {
				final Type type = types[i];
				if ( type.isComponentType() ) {
					final CompositeType compositeType = (CompositeType) type;
					// need to extract the component values and check for subtype replacements...
					final Type[] subtypes = compositeType.getSubtypes();
					final Object[] origComponentValues = currentOriginal == null
							? new Object[subtypes.length]
							: compositeType.getPropertyValues( currentOriginal, session );
					final Object[] targetComponentValues = target[i] == null
							? new Object[subtypes.length]
							: compositeType.getPropertyValues( target[i], session );
					final Object[] objects = replaceAssociations(
							origComponentValues,
							targetComponentValues,
							subtypes,
							session,
							null,
							copyCache,
							foreignKeyDirection
					);
					if ( target[i] != null && compositeType instanceof ComponentType ) {
						final ComponentType componentType = (ComponentType) compositeType;
						if ( !componentType.isMutable() || componentType.isCompositeUserType() ) {
							final EmbeddableInstantiator instantiator = ( (ComponentType) compositeType ).getMappingModelPart()
									.getEmbeddableTypeDescriptor()
									.getRepresentationStrategy()
									.getInstantiator();
							target[i] = instantiator.instantiate( () -> objects, session.getSessionFactory() );
						}
						else {
							compositeType.setPropertyValues( target[i], objects );
						}
					}
					copied[i] = target[i];
				}
				else if ( !type.isAssociationType() ) {
					copied[i] = target[i];
				}
				else {
					copied[i] = types[i].replace( currentOriginal, target[i], session, owner, copyCache, foreignKeyDirection );
				}
			}
		}
		return copied;
	}

}
