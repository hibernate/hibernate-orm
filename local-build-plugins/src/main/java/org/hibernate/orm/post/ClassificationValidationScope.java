/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/// Identifies the artifact boundary from which a classified declaration
/// participates in validation.
///
/// Artifact roles are validation context, not declaration classifications.
/// In particular, a public declaration in a provider artifact remains that
/// artifact's API while depending on the supported API and SPI of an upstream
/// Hibernate platform artifact.
///
/// @author Steve Ebersole
final class ClassificationValidationScope {
	private static final ClassificationValidationScope PLATFORM_ONLY =
			new ClassificationValidationScope( Collections.emptySet() );

	private final Set<String> providerArtifacts;

	private ClassificationValidationScope(Set<String> providerArtifacts) {
		this.providerArtifacts = Collections.unmodifiableSet( new TreeSet<>( providerArtifacts ) );
	}

	static ClassificationValidationScope platformOnly() {
		return PLATFORM_ONLY;
	}

	static ClassificationValidationScope withProviderArtifacts(Collection<File> artifacts) {
		final Map<String, String> pathsByArtifact = new LinkedHashMap<>();
		for ( File artifact : artifacts ) {
			final String name = artifact.getName();
			final String path = artifact.getAbsolutePath();
			final String previous = pathsByArtifact.putIfAbsent( name, path );
			if ( previous != null && !previous.equals( path ) ) {
				throw new IllegalArgumentException(
						"Provider artifact identity '" + name + "' is ambiguous between " + previous + " and " + path
				);
			}
		}
		return pathsByArtifact.isEmpty()
				? PLATFORM_ONLY
				: new ClassificationValidationScope( pathsByArtifact.keySet() );
	}

	boolean isProvider(ClassificationModel.Element element) {
		return providerArtifacts.contains( element.getArtifact() );
	}

	boolean isProviderToPlatform(
			ClassificationModel.Element source,
			ClassificationModel.Element target) {
		return isProvider( source ) && !isProvider( target );
	}

	boolean isPlatformToProvider(
			ClassificationModel.Element source,
			ClassificationModel.Element target) {
		return !isProvider( source ) && isProvider( target );
	}
}
