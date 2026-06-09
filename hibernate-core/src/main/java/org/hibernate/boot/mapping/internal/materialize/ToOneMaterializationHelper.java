/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import org.hibernate.boot.mapping.internal.sources.ToOneSource;
import org.hibernate.engine.FetchStyle;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.ToOne;

import jakarta.persistence.FetchType;

/// Shared materialization helpers for to-one mapping values.
///
/// This keeps common `ToOne` value shaping out of attribute binders while
/// identifier materialization still creates transitional mapping objects
/// directly.
///
/// @since 9.0
/// @author Steve Ebersole
public final class ToOneMaterializationHelper {
	private ToOneMaterializationHelper() {
	}

	public static void applyFetchMode(ToOneSource source, ToOne value) {
		applyFetchMode( source, value, value.isLazy() ? FetchType.LAZY : FetchType.EAGER );
	}

	public static void applyFetchMode(ToOneSource source, ToOne value, FetchType fetchType) {
		final org.hibernate.annotations.FetchMode fetchMode = source.hibernateFetchMode();
		if ( fetchMode == null ) {
			value.setFetchStyle( fetchType == FetchType.LAZY ? FetchStyle.SELECT : FetchStyle.JOIN );
			return;
		}

		value.setFetchStyle( fetchStyle( fetchMode ) );
		if ( fetchMode == org.hibernate.annotations.FetchMode.JOIN ) {
			value.setLazy( false );
			value.setUnwrapProxy( false );
		}
	}

	public static void applyFetchMode(ToOneSource source, ToOne value, PersistentClass ownerBinding) {
		applyFetchMode( source, value );
		if ( source.hibernateFetchMode() == org.hibernate.annotations.FetchMode.SUBSELECT ) {
			ownerBinding.setSubselectLoadableAttributes( true );
		}
	}

	public static void applyFetchMode(
			ToOneSource source,
			ToOne value,
			PersistentClass ownerBinding,
			FetchType fetchType) {
		applyFetchMode( source, value, fetchType );
		if ( source.hibernateFetchMode() == org.hibernate.annotations.FetchMode.SUBSELECT ) {
			ownerBinding.setSubselectLoadableAttributes( true );
		}
	}

	private static FetchStyle fetchStyle(org.hibernate.annotations.FetchMode fetchMode) {
		return switch ( fetchMode ) {
			case JOIN -> FetchStyle.JOIN;
			case SELECT -> FetchStyle.SELECT;
			case SUBSELECT -> FetchStyle.SUBSELECT;
		};
	}
}
