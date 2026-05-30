/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity;

import org.hibernate.Internal;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.type.Type;

import jakarta.annotation.Nullable;

/**
 * Operations for searching an array of property values for modified elements.
 */
@Internal
class DirtyHelper {
	/**
	 * Determine if any of the given field values are dirty, returning an array containing
	 * indices of the dirty fields.
	 * <p>
	 * If it is determined that no fields are dirty, null is returned.
	 *
	 * @param propertyTypes The property types that are dirty checkable. null entry for non-dirty checkable properties
	 * @param currentState The current state of the entity
	 * @param previousState The baseline state of the entity
	 * @param includeColumns Columns to be included in the dirty checking, per property
	 * @param session The session from which the dirty check request originated.
	 *
	 * @return Array containing indices of the dirty properties, or null if no properties considered dirty.
	 */
	public static int[] findDirty(
			@Nullable Type[] propertyTypes,
			final Object[] currentState,
			final Object[] previousState,
			final boolean[][] includeColumns,
			final SharedSessionContractImplementor session) {
		int[] results = null;
		int count = 0;
		int span = propertyTypes.length;
		for ( int i = 0; i < span; i++ ) {
			if ( isDirty( propertyTypes, currentState, previousState, includeColumns, session, i ) ) {
				if ( results == null ) {
					results = new int[span];
				}
				results[count++] = i;
			}
		}
		return count == 0 ? null : ArrayHelper.trim( results, count );
	}

	private static boolean isDirty(
			@Nullable Type[] propertyTypes,
			Object[] currentState,
			Object[] previousState,
			boolean[][] includeColumns,
			SharedSessionContractImplementor session, int i) {
		final Type propertyType;
		if ( currentState[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY
				|| ( propertyType = propertyTypes[i] ) == null ) {
			return false;
		}
		else if ( previousState[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
			return true;
		}
		else {
			return propertyType.isDirty( previousState[i], currentState[i], includeColumns[i], session );
		}
	}

	/**
	 * Determine if any of the given field values are modified, returning an array containing
	 * indices of the modified fields.
	 * <p>
	 * If it is determined that no fields are dirty, null is returned.
	 *
	 * @param propertyTypes The property types
	 * @param propertyCheckability Whether each property participates in the check
	 * @param currentState The current state of the entity
	 * @param previousState The baseline state of the entity
	 * @param includeColumns Columns to be included in the mod checking, per property
	 * @param includeProperties Array of property indices that identify which properties participate in check
	 * @param session The session from which the dirty check request originated.
	 *
	 * @return Array containing indices of the modified properties, or null if no properties considered modified.
	 **/
	public static int[] findModified(
			final Type[] propertyTypes,
			final boolean[] propertyCheckability,
			final Object[] currentState,
			final Object[] previousState,
			final boolean[][] includeColumns,
			final boolean[] includeProperties,
			final SharedSessionContractImplementor session) {
		int[] results = null;
		int count = 0;
		int span = propertyTypes.length;

		for ( int i = 0; i < span; i++ ) {
			if ( isModified(
					propertyTypes,
					propertyCheckability,
					currentState,
					previousState,
					includeColumns,
					includeProperties,
					session,
					i ) ) {
				if ( results == null ) {
					results = new int[ span ];
				}
				results[ count++ ] = i;
			}
		}

		if ( count == 0 ) {
			return null;
		}
		else {
			int[] trimmed = new int[ count ];
			System.arraycopy( results, 0, trimmed, 0, count );
			return trimmed;
		}
	}

	private static boolean isModified(
			Type[] propertyTypes,
			boolean[] propertyCheckability,
			Object[] currentState,
			Object[] previousState,
			boolean[][] includeColumns,
			boolean[] includeProperties,
			SharedSessionContractImplementor session,
			int i) {
		return currentState[i] != LazyPropertyInitializer.UNFETCHED_PROPERTY
			&& includeProperties[i]
			&& propertyCheckability[i]
			&& propertyTypes[i].isModified( previousState[i], currentState[i], includeColumns[i], session );
	}
}
