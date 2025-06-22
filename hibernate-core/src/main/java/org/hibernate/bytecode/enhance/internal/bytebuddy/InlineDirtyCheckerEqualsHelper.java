/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.util.Objects;

import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;

public final class InlineDirtyCheckerEqualsHelper {

	public static boolean areEquals(
			PersistentAttributeInterceptable persistentAttributeInterceptable,
			String fieldName,
			Object a,
			Object b) {
		final PersistentAttributeInterceptor persistentAttributeInterceptor = persistentAttributeInterceptable.$$_hibernate_getInterceptor();
		if ( persistentAttributeInterceptor != null
				&& !persistentAttributeInterceptor.isAttributeLoaded( fieldName ) ) {
			return false;
		}
		return Objects.deepEquals( a, b );
	}

	public static boolean areEquals(
			PersistentAttributeInterceptable persistentAttributeInterceptable,
			String fieldName,
			boolean a,
			boolean b) {
		final PersistentAttributeInterceptor persistentAttributeInterceptor = persistentAttributeInterceptable.$$_hibernate_getInterceptor();
		if ( persistentAttributeInterceptor != null
				&& !persistentAttributeInterceptor.isAttributeLoaded( fieldName ) ) {
			return false;
		}
		return a == b;
	}

	public static boolean areEquals(
			PersistentAttributeInterceptable persistentAttributeInterceptable,
			String fieldName,
			byte a,
			byte b) {
		final PersistentAttributeInterceptor persistentAttributeInterceptor = persistentAttributeInterceptable.$$_hibernate_getInterceptor();
		if ( persistentAttributeInterceptor != null
				&& !persistentAttributeInterceptor.isAttributeLoaded( fieldName ) ) {
			return false;
		}
		return a == b;
	}

	public static boolean areEquals(
			PersistentAttributeInterceptable persistentAttributeInterceptable,
			String fieldName,
			short a,
			short b) {
		final PersistentAttributeInterceptor persistentAttributeInterceptor = persistentAttributeInterceptable.$$_hibernate_getInterceptor();
		if ( persistentAttributeInterceptor != null
				&& !persistentAttributeInterceptor.isAttributeLoaded( fieldName ) ) {
			return false;
		}
		return a == b;
	}

	public static boolean areEquals(
			PersistentAttributeInterceptable persistentAttributeInterceptable,
			String fieldName,
			char a,
			char b) {
		final PersistentAttributeInterceptor persistentAttributeInterceptor = persistentAttributeInterceptable.$$_hibernate_getInterceptor();
		if ( persistentAttributeInterceptor != null
				&& !persistentAttributeInterceptor.isAttributeLoaded( fieldName ) ) {
			return false;
		}
		return a == b;
	}

	public static boolean areEquals(
			PersistentAttributeInterceptable persistentAttributeInterceptable,
			String fieldName,
			int a,
			int b) {
		final PersistentAttributeInterceptor persistentAttributeInterceptor = persistentAttributeInterceptable.$$_hibernate_getInterceptor();
		if ( persistentAttributeInterceptor != null
				&& !persistentAttributeInterceptor.isAttributeLoaded( fieldName ) ) {
			return false;
		}
		return a == b;
	}

	public static boolean areEquals(
			PersistentAttributeInterceptable persistentAttributeInterceptable,
			String fieldName,
			long a,
			long b) {
		final PersistentAttributeInterceptor persistentAttributeInterceptor = persistentAttributeInterceptable.$$_hibernate_getInterceptor();
		if ( persistentAttributeInterceptor != null
				&& !persistentAttributeInterceptor.isAttributeLoaded( fieldName ) ) {
			return false;
		}
		return a == b;
	}

	public static boolean areEquals(
			PersistentAttributeInterceptable persistentAttributeInterceptable,
			String fieldName,
			float a,
			float b) {
		final PersistentAttributeInterceptor persistentAttributeInterceptor = persistentAttributeInterceptable.$$_hibernate_getInterceptor();
		if ( persistentAttributeInterceptor != null
				&& !persistentAttributeInterceptor.isAttributeLoaded( fieldName ) ) {
			return false;
		}
		return a == b;
	}

	public static boolean areEquals(
			PersistentAttributeInterceptable persistentAttributeInterceptable,
			String fieldName,
			double a,
			double b) {
		final PersistentAttributeInterceptor persistentAttributeInterceptor = persistentAttributeInterceptable.$$_hibernate_getInterceptor();
		if ( persistentAttributeInterceptor != null
				&& !persistentAttributeInterceptor.isAttributeLoaded( fieldName ) ) {
			return false;
		}
		return a == b;
	}
}
