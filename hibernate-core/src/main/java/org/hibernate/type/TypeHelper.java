/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.bytecode.instrumentation.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;
import org.hibernate.tuple.NonIdentifierAttribute;

/**
 * Collection of convenience methods relating to operations across arrays of types...
 *
 * @author Steve Ebersole
 */
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
			final SessionImplementor session) {
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
	 * Apply the {@link Type#beforeAssemble} operation across a series of values.
	 *
	 * @param row The values
	 * @param types The value types
	 * @param session The originating session
	 */
	public static void beforeAssemble(
			final Serializable[] row,
			final Type[] types,
			final SessionImplementor session) {
		for ( int i = 0; i < types.length; i++ ) {
			if ( row[i] != LazyPropertyInitializer.UNFETCHED_PROPERTY
				&& row[i] != PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
				types[i].beforeAssemble( row[i], session );
			}
		}
	}

	/**
	 * Apply the {@link Type#assemble} operation across a series of values.
	 *
	 * @param row The values
	 * @param types The value types
	 * @param session The originating session
	 * @param owner The entity "owning" the values
	 * @return The assembled state
	 */
	public static Object[] assemble(
			final Serializable[] row,
			final Type[] types,
			final SessionImplementor session,
			final Object owner) {
		Object[] assembled = new Object[row.length];
		for ( int i = 0; i < types.length; i++ ) {
			if ( row[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY || row[i] == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
				assembled[i] = row[i];
			}
			else {
				assembled[i] = types[i].assemble( row[i], session, owner );
			}
		}
		return assembled;
	}

	/**
	 * Apply the {@link Type#disassemble} operation across a series of values.
	 *
	 * @param row The values
	 * @param types The value types
	 * @param nonCacheable An array indicating which values to include in the disassembled state
	 * @param session The originating session
	 * @param owner The entity "owning" the values
	 *
	 * @return The disassembled state
	 */
	public static Serializable[] disassemble(
			final Object[] row,
			final Type[] types,
			final boolean[] nonCacheable,
			final SessionImplementor session,
			final Object owner) {
		Serializable[] disassembled = new Serializable[row.length];
		for ( int i = 0; i < row.length; i++ ) {
			if ( nonCacheable!=null && nonCacheable[i] ) {
				disassembled[i] = LazyPropertyInitializer.UNFETCHED_PROPERTY;
			}
			else if ( row[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY || row[i] == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
				disassembled[i] = (Serializable) row[i];
			}
			else {
				disassembled[i] = types[i].disassemble( row[i], session, owner );
			}
		}
		return disassembled;
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
			final SessionImplementor session,
			final Object owner,
			final Map copyCache) {
		Object[] copied = new Object[original.length];
		for ( int i = 0; i < types.length; i++ ) {
			if ( original[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY
				|| original[i] == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
				copied[i] = target[i];
			}
			else if ( target[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
				// Should be no need to check for target[i] == PropertyAccessStrategyBackRefImpl.UNKNOWN
				// because PropertyAccessStrategyBackRefImpl.get( object ) returns
				// PropertyAccessStrategyBackRefImpl.UNKNOWN, so target[i] == original[i].
				//
				// We know from above that original[i] != LazyPropertyInitializer.UNFETCHED_PROPERTY &&
				// original[i] != PropertyAccessStrategyBackRefImpl.UNKNOWN;
				// This is a case where the entity being merged has a lazy property
				// that has been initialized. Copy the initialized value from original.
				if ( types[i].isMutable() ) {
					copied[i] = types[i].deepCopy( original[i], session.getFactory() );
				}
				else {
					copied[i] = original[i];
				}
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
			final SessionImplementor session,
			final Object owner,
			final Map copyCache,
			final ForeignKeyDirection foreignKeyDirection) {
		Object[] copied = new Object[original.length];
		for ( int i = 0; i < types.length; i++ ) {
			if ( original[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY
				|| original[i] == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
				copied[i] = target[i];
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
	 * <p/>
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
			final SessionImplementor session,
			final Object owner,
			final Map copyCache,
			final ForeignKeyDirection foreignKeyDirection) {
		Object[] copied = new Object[original.length];
		for ( int i = 0; i < types.length; i++ ) {
			if ( original[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY
					|| original[i] == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
				copied[i] = target[i];
			}
			else if ( types[i].isComponentType() ) {
				// need to extract the component values and check for subtype replacements...
				CompositeType componentType = ( CompositeType ) types[i];
				Type[] subtypes = componentType.getSubtypes();
				Object[] origComponentValues = original[i] == null ? new Object[subtypes.length] : componentType.getPropertyValues( original[i], session );
				Object[] targetComponentValues = target[i] == null ? new Object[subtypes.length] : componentType.getPropertyValues( target[i], session );
				replaceAssociations( origComponentValues, targetComponentValues, subtypes, session, null, copyCache, foreignKeyDirection );
				copied[i] = target[i];
			}
			else if ( !types[i].isAssociationType() ) {
				copied[i] = target[i];
			}
			else {
				copied[i] = types[i].replace( original[i], target[i], session, owner, copyCache, foreignKeyDirection );
			}
		}
		return copied;
	}

	/**
	 * Determine if any of the given field values are dirty, returning an array containing
	 * indices of the dirty fields.
	 * <p/>
	 * If it is determined that no fields are dirty, null is returned.
	 *
	 * @param properties The property definitions
	 * @param currentState The current state of the entity
	 * @param previousState The baseline state of the entity
	 * @param includeColumns Columns to be included in the dirty checking, per property
	 * @param anyUninitializedProperties Does the entity currently hold any uninitialized property values?
	 * @param session The session from which the dirty check request originated.
	 * 
	 * @return Array containing indices of the dirty properties, or null if no properties considered dirty.
	 */
	public static int[] findDirty(
			final NonIdentifierAttribute[] properties,
			final Object[] currentState,
			final Object[] previousState,
			final boolean[][] includeColumns,
			final boolean anyUninitializedProperties,
			final SessionImplementor session) {
		int[] results = null;
		int count = 0;
		int span = properties.length;

		for ( int i = 0; i < span; i++ ) {
			final boolean dirty = currentState[i] != LazyPropertyInitializer.UNFETCHED_PROPERTY
					&& properties[i].isDirtyCheckable( anyUninitializedProperties )
					&& properties[i].getType().isDirty( previousState[i], currentState[i], includeColumns[i], session );
			if ( dirty ) {
				if ( results == null ) {
					results = new int[span];
				}
				results[count++] = i;
			}
		}

		if ( count == 0 ) {
			return null;
		}
		else {
			int[] trimmed = new int[count];
			System.arraycopy( results, 0, trimmed, 0, count );
			return trimmed;
		}
	}

	/**
	 * Determine if any of the given field values are modified, returning an array containing
	 * indices of the modified fields.
	 * <p/>
	 * If it is determined that no fields are dirty, null is returned.
	 *
	 * @param properties The property definitions
	 * @param currentState The current state of the entity
	 * @param previousState The baseline state of the entity
	 * @param includeColumns Columns to be included in the mod checking, per property
	 * @param anyUninitializedProperties Does the entity currently hold any uninitialized property values?
	 * @param session The session from which the dirty check request originated.
	 *
	 * @return Array containing indices of the modified properties, or null if no properties considered modified.
	 */
	public static int[] findModified(
			final NonIdentifierAttribute[] properties,
			final Object[] currentState,
			final Object[] previousState,
			final boolean[][] includeColumns,
			final boolean anyUninitializedProperties,
			final SessionImplementor session) {
		int[] results = null;
		int count = 0;
		int span = properties.length;

		for ( int i = 0; i < span; i++ ) {
			final boolean modified = currentState[i]!=LazyPropertyInitializer.UNFETCHED_PROPERTY
					&& properties[i].isDirtyCheckable(anyUninitializedProperties)
					&& properties[i].getType().isModified( previousState[i], currentState[i], includeColumns[i], session );

			if ( modified ) {
				if ( results == null ) {
					results = new int[span];
				}
				results[count++] = i;
			}
		}

		if ( count == 0 ) {
			return null;
		}
		else {
			int[] trimmed = new int[count];
			System.arraycopy( results, 0, trimmed, 0, count );
			return trimmed;
		}
	}
}
