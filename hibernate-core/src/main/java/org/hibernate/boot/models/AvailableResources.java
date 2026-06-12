/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models;

import java.util.Collection;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.models.spi.ClassDetails;

/// Model resources available to categorization.
///
/// The contract separates the source material into three buckets:
///
/// * class details for explicitly visible managed classes and dynamic model types
/// * class details for package metadata, represented by {@code package-info}
///   class details
/// * already-bound XML mapping documents
///
/// @since 9.0
/// @author Steve Ebersole
public interface AvailableResources {
	Collection<ClassDetails> managedClassDetails();

	Collection<ClassDetails> packageDetails();

	Collection<Binding<JaxbEntityMappingsImpl>> xmlMappings();
}
