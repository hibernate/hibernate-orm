/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.util.Objects;

import org.hibernate.engine.spi.PersistentAttributeInterceptable;

public final class InlineDirtyCheckerEqualsHelper {

	private static boolean isLoaded(PersistentAttributeInterceptable persistentAttributeInterceptable, String fieldName) {
		final var persistentAttributeInterceptor = persistentAttributeInterceptable.$$_hibernate_getInterceptor();
		return persistentAttributeInterceptor == null
			|| persistentAttributeInterceptor.isAttributeLoaded( fieldName );
	}

	public static boolean areEquals(
			PersistentAttributeInterceptable persistentAttributeInterceptable,
			String fieldName,
			Object a,
			Object b) {
		return isLoaded( persistentAttributeInterceptable, fieldName )
			&& Objects.deepEquals( a, b );
	}

	public static boolean areEquals(
			PersistentAttributeInterceptable persistentAttributeInterceptable,
			String fieldName,
			boolean a,
			boolean b) {
		return isLoaded( persistentAttributeInterceptable, fieldName )
			&& a == b;
	}

	public static boolean areEquals(
			PersistentAttributeInterceptable persistentAttributeInterceptable,
			String fieldName,
			byte a,
			byte b) {
		return isLoaded( persistentAttributeInterceptable, fieldName )
			&& a == b;
	}

	public static boolean areEquals(
			PersistentAttributeInterceptable persistentAttributeInterceptable,
			String fieldName,
			short a,
			short b) {
		return isLoaded( persistentAttributeInterceptable, fieldName )
			&& a == b;
	}

	public static boolean areEquals(
			PersistentAttributeInterceptable persistentAttributeInterceptable,
			String fieldName,
			char a,
			char b) {
		return isLoaded( persistentAttributeInterceptable, fieldName )
			&& a == b;
	}

	public static boolean areEquals(
			PersistentAttributeInterceptable persistentAttributeInterceptable,
			String fieldName,
			int a,
			int b) {
		return isLoaded( persistentAttributeInterceptable, fieldName )
			&& a == b;
	}

	public static boolean areEquals(
			PersistentAttributeInterceptable persistentAttributeInterceptable,
			String fieldName,
			long a,
			long b) {
		return isLoaded( persistentAttributeInterceptable, fieldName )
			&& a == b;
	}

	public static boolean areEquals(
			PersistentAttributeInterceptable persistentAttributeInterceptable,
			String fieldName,
			float a,
			float b) {
		return isLoaded( persistentAttributeInterceptable, fieldName )
			&& a == b;
	}

	public static boolean areEquals(
			PersistentAttributeInterceptable persistentAttributeInterceptable,
			String fieldName,
			double a,
			double b) {
		return isLoaded( persistentAttributeInterceptable, fieldName )
			&& a == b;
	}
}
