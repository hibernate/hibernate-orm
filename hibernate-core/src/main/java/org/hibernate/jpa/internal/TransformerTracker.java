/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.internal;

import jakarta.persistence.spi.PersistenceUnitInfo;

import java.util.HashSet;
import java.util.Set;

/// Used to help track which persistence units have already had a
/// [org.hibernate.bytecode.spi.ClassTransformer] applied.
///
/// @author Steve Ebersole
public class TransformerTracker {
	private static final Set<TransformerKey> UNITS_WITH_TRANSFORMER = new HashSet<>();

	/**
	 * Defines the matching condition between persistence units.
	 */
	public record TransformerKey(String puName, String loaderName) {
		public static TransformerKey from(PersistenceUnitInfo persistenceUnitInfo) {
			return new TransformerKey(
					persistenceUnitInfo.getPersistenceUnitName(),
					persistenceUnitInfo.getClassLoader().getName()
			);
		}
	}

	/// Track the intent to supply a [org.hibernate.bytecode.spi.ClassTransformer] to
	/// the container for the indicated persistence unit.
	/// Returns whether the caller should take responsibility for supplying the transformer.
	///
	/// @return {@code true} if no transformer is already tracked for the given
	/// persistence unit; {@code false} means that transformer is already tracked for the
	/// given persistence unit and that the caller should not supply one.
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static boolean canSupplyTransformer(TransformerKey key) {
		return UNITS_WITH_TRANSFORMER.add(key);
	}

	private TransformerTracker() {
	}
}
