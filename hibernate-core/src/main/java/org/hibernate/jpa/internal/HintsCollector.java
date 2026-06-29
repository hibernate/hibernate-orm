/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.internal;

import java.util.HashSet;
import java.util.Set;

import jakarta.annotation.Nonnull;
import org.hibernate.HibernateException;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.jpa.SpecHints;

import static java.util.Collections.unmodifiableSet;

/**
 * Collects all available hints for use the Jakarta Persistence hint system
 *
 * @author Steve Ebersole
 */
public class HintsCollector {
	private static final Set<String> HINTS = buildHintsSet();

	public static Set<String> getDefinedHints() {
		return HINTS;
	}

	@Nonnull
	private static Set<String> buildHintsSet() {
		final HashSet<String> hints = new HashSet<>();
		applyHints( hints, HibernateHints.class );
		applyHints( hints, SpecHints.class );
		return unmodifiableSet( hints );
	}

	private static void applyHints(@Nonnull HashSet<String> hints, @Nonnull Class<?> hintsClass) {
		for ( final var field : hintsClass.getDeclaredFields() ) {
			if ( field.getName().startsWith( "HINT_" )
					&& field.getType().equals( String.class ) ) {
				// the field's value is the hint name
				try {
					hints.add( (String) field.get( hintsClass ) );
				}
				catch (IllegalAccessException e) {
					throw new HibernateException(
							"Unable to generate set of all hints - " + hintsClass.getName(),
							e
					);
				}
			}

		}
	}
}
