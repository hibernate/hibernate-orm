/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.dialectprovider;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

/// Configures validation and contract testing for an external Hibernate ORM
/// Dialect provider.
///
/// Provider packages identify provider-owned bytecode. Contract profiles are
/// provider-written implementations consumed by the generated JUnit bridge.
///
/// @author Steve Ebersole
/// @since 8.0
public abstract class HibernateDialectProviderExtension {
	/// Package prefixes which contain provider-owned classes.
	public abstract ListProperty<String> getProviderPackages();

	/// Ordered binary names of provider-written Dialect contract profiles.
	public abstract ListProperty<String> getContractProfiles();

	/// The exact Hibernate ORM version used by the provider.
	public abstract Property<String> getHibernateVersion();

	/// Selects a local classification document and disables network resolution.
	public abstract RegularFileProperty getClassificationMetadataFile();

	/// Base URL containing release-family classification metadata.
	public abstract Property<String> getClassificationMetadataBaseUrl();

	/// Provider-owned artifacts to analyze.
	public abstract ConfigurableFileCollection getProviderArtifacts();

	/// Resolved upstream Hibernate ORM artifacts to analyze.
	public abstract ConfigurableFileCollection getHibernateArtifacts();

	/// Whether provider verification participates in the Java `check` lifecycle.
	public abstract Property<Boolean> getAttachToCheck();

	/// Configure whether warning findings should fail provider-boundary
	/// validation.
	///
	/// Set this property to `true` for a strict build. The setting changes only
	/// the task outcome; reports retain each finding's declared `WARNING`
	/// severity.
	public abstract Property<Boolean> getWarningsAsErrors();
}
