/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.materialize;

import org.hibernate.boot.models.mapping.internal.sources.ToOneSource;
import org.hibernate.engine.FetchStyle;
import org.hibernate.mapping.ManyToOne;

/// Shared materialization helpers for to-one mapping values.
///
/// This keeps common `ManyToOne` value shaping out of attribute binders while
/// identifier materialization still creates transitional mapping objects
/// directly.
///
/// @since 9.0
/// @author Steve Ebersole
public final class ToOneMaterializationHelper {
	private ToOneMaterializationHelper() {
	}

	public static void applyFetchMode(ToOneSource source, ManyToOne value) {
		final org.hibernate.annotations.Fetch fetch = source.hibernateFetch();
		if ( fetch == null ) {
			value.setFetchStyle( value.isLazy() ? FetchStyle.SELECT : FetchStyle.JOIN );
			return;
		}

		value.setFetchStyle( fetchStyle( fetch.value() ) );
		if ( fetch.value() == org.hibernate.annotations.FetchMode.JOIN ) {
			value.setLazy( false );
			value.setUnwrapProxy( false );
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
