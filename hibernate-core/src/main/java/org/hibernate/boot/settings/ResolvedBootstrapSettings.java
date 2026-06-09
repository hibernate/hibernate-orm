/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.settings;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/// Resolved bootstrap settings relevant to model source collection,
/// categorization, and binding.
///
/// The raw configuration map remains available for settings consumed downstream,
/// while grouped, frequently used bootstrap decisions are exposed as named
/// values.  This keeps the early boot flow from repeatedly interpreting loosely
/// typed settings.
///
/// General-purpose configuration should remain in [#configurationValues()] until
/// a later bootstrap component needs it.
///
/// @since 9.0
/// @author Steve Ebersole
public record ResolvedBootstrapSettings(
		/// The normalized raw configuration values available to downstream
		/// bootstrap components.
		///
		/// Keys are represented as strings and the map is defensively copied by
		/// the canonical constructor.
		Map<String, Object> configurationValues,

		/// Whether this bootstrap originated from a Jakarta Persistence entry
		/// point.
		///
		/// This lets later stages distinguish JPA bootstrap defaults and
		/// compatibility behavior from native Hibernate bootstrap.
		boolean jpaBootstrap,

		/// Resolved settings used while processing mapping sources.
		ResolvedMappingSettings mappingSettings) {

	/// Exposes immutable snapshots.
	public ResolvedBootstrapSettings {
		configurationValues = Collections.unmodifiableMap( new LinkedHashMap<>(
				Objects.requireNonNull( configurationValues )
		) );
		Objects.requireNonNull( mappingSettings );
	}
}
