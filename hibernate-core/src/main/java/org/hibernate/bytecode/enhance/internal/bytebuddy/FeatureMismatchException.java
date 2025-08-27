/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import net.bytebuddy.description.type.TypeDescription;
import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.bytecode.enhance.spi.EnhancementInfo;

import java.util.Locale;

/**
 * Indicates a mismatch in either {@linkplain EnhancementInfo#includesDirtyChecking() dirty tracking}
 * or {@linkplain EnhancementInfo#includesAssociationManagement() association management}
 * between consecutive attempts to enhance a class.
 *
 * @author Steve Ebersole
 */
public class FeatureMismatchException extends EnhancementException {
	public enum Feature { DIRTY_CHECK, ASSOCIATION_MANAGEMENT }

	private final String className;
	private final Feature mismatchedFeature;
	private final boolean previousValue;

	public FeatureMismatchException(
			String className,
			Feature mismatchedFeature,
			boolean previousValue) {
		super( String.format(
				Locale.ROOT,
				"Support for %s was enabled during enhancement, but `%s` was previously enhanced with that support %s.",
				featureText( mismatchedFeature ),
				className,
				decode( previousValue )
		) );
		this.className = className;
		this.mismatchedFeature = mismatchedFeature;
		this.previousValue = previousValue;
	}

	public String getClassName() {
		return className;
	}

	public Feature getMismatchedFeature() {
		return mismatchedFeature;
	}

	public boolean wasPreviouslyEnabled() {
		return previousValue;
	}

	public static void checkFeatureEnablement(
			TypeDescription managedCtClass,
			Feature feature,
			boolean currentlyEnabled,
			boolean previouslyEnabled) {
		if ( currentlyEnabled != previouslyEnabled ) {
			throw new FeatureMismatchException( managedCtClass.getName(), feature, previouslyEnabled );
		}
	}

	private static String featureText(Feature mismatchedFeature) {
		return switch ( mismatchedFeature ) {
			case DIRTY_CHECK -> "inline dirty checking";
			case ASSOCIATION_MANAGEMENT -> "bidirectional association management";
		};
	}

	private static String decode(boolean previousValue) {
		return previousValue ? "enabled" : "disabled";
	}
}
