/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

/// Configures release-family metadata used for migration compatibility and
/// migration-guide review.
///
/// @author Steve Ebersole
/// @since 8.0
public abstract class MigrationCompatibilityExtension {
	/// The `X.Y` family used for compatibility enforcement.
	public abstract Property<String> getBaselineFamily();

	/// The independently selected `X.Y` family used for advisory review.
	public abstract Property<String> getReviewFamily();

	/// Base URL containing family-scoped classification metadata.
	public abstract Property<String> getClassificationMetadataBaseUrl();

	/// Selects a local enforcement-baseline document and disables network
	/// resolution for that input.
	public abstract RegularFileProperty getBaselineClassificationMetadataFile();

	/// Selects a local review-baseline document and disables network resolution
	/// for that input.
	public abstract RegularFileProperty getReviewClassificationMetadataFile();

	/// Explicitly enables the guarded first-publication seed operation.
	public abstract Property<Boolean> getBootstrapBaseline();
}
