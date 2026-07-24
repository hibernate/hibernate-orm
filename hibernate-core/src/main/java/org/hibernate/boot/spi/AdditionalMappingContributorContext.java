/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import org.hibernate.Incubating;
import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.dialect.Dialect;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.type.spi.TypeConfiguration;

/// Contextual services available to an [AdditionalMappingContributor].
///
/// @since 9.0
/// @author Steve Ebersole
@Incubating
public interface AdditionalMappingContributorContext {
	/// Access to the Hibernate Models context
	ModelsContext getModelsContext();

	/// Access to class-loading features
	ClassLoaderAccess getClassLoaderAccess();

	/// Access to resource-stream location
	ResourceStreamLocator getResourceStreamLocator();

	/// The effective Dialect
	Dialect getDialect();

	/// The effective TypeConfiguration
	TypeConfiguration getTypeConfiguration();
}
