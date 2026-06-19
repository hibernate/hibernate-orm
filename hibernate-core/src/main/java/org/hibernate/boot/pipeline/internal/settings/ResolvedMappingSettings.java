/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal.settings;

import java.util.Collections;
import java.util.List;

import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.mapping.BindingSettings;

import jakarta.persistence.FetchType;

/// Resolved settings used while processing mapping sources.
///
/// These settings are separated from the bootstrap envelope because they apply
/// specifically to XML mapping collection/reading and binding decisions.
///
/// @since 9.0
/// @author Steve Ebersole
public record ResolvedMappingSettings(
		/// Whether XML mapping documents should be processed.
		///
		/// When disabled, explicitly named mapping files and conventionally
		/// included `META-INF/orm.xml` resources should be ignored by source
		/// collection.
		///
		/// @see org.hibernate.cfg.MappingSettings#XML_MAPPING_ENABLED
		boolean xmlMappingEnabled,

		/// Whether XML mapping documents should be validated while being read.
		///
		/// This setting only matters when XML mappings are enabled and present.
		///
		/// @see org.hibernate.cfg.MappingSettings#VALIDATE_XML
		boolean validateXml,

		/// Default fetch type to apply to to-one associations that request the
		/// Jakarta Persistence `DEFAULT` fetch type.
		FetchType defaultToOneFetchType,

		/// Whether joined-subclass hierarchies should create an implicit
		/// discriminator when no explicit discriminator mapping is present.
		///
		/// @see org.hibernate.cfg.MappingSettings#IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS
		boolean createImplicitDiscriminatorsForJoinedInheritance,

		/// Whether explicit discriminator mappings on joined-subclass hierarchies
		/// should be ignored.
		///
		/// @see org.hibernate.cfg.MappingSettings#IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS
		boolean ignoreExplicitDiscriminatorsForJoinedInheritance,

		/// Cache region declarations extracted from configuration settings such
		/// as `hibernate.classcache.*` and `hibernate.collectioncache.*`.
		///
		/// The list is defensively copied by the canonical constructor.
		List<CacheRegionDefinition> cacheRegionDefinitions) implements BindingSettings {

	public ResolvedMappingSettings(
			boolean xmlMappingEnabled,
			boolean validateXml,
			FetchType defaultToOneFetchType,
			List<CacheRegionDefinition> cacheRegionDefinitions) {
		this(
				xmlMappingEnabled,
				validateXml,
				defaultToOneFetchType,
				false,
				false,
				cacheRegionDefinitions
		);
	}

	/// Normalizes nullable collection inputs and exposes immutable snapshots.
	public ResolvedMappingSettings {
		defaultToOneFetchType = defaultToOneFetchType == null ? FetchType.EAGER : defaultToOneFetchType;
		cacheRegionDefinitions = cacheRegionDefinitions == null
				? Collections.emptyList()
				: List.copyOf( cacheRegionDefinitions );
	}
}
