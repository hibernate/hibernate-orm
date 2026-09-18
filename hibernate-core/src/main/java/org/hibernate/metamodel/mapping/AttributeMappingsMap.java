/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.Consumer;

import org.hibernate.Incubating;
import org.hibernate.metamodel.mapping.internal.EmptyAttributeMappingsMap;
import org.hibernate.metamodel.mapping.internal.ImmutableAttributeMappingsMap;

/**
 * Similar to {@link AttributeMappingsList}, this is essentially
 * an immutable Map of AttributeMapping(s), allowing iteration of
 * all mappings but also retrieval by name (a String key).
 * Exposing a custom interface is more suitable to our needs than
 * reusing the stock Map API; it expresses the immutable nature of
 * this structure, and might allow us to extend it with additional
 * convenience methods such as needs evolve.
 * And additional reason for the custom interface is to allow
 * custom implementations which can be highly optimised as
 * necessary for our specific needs; for example the
 * implementation {@link ImmutableAttributeMappingsMap}
 * is able to avoid caching problems related to JDK-8180450, which would
 * not have been possible with a standard generic container.
 *
 * @since 6.2
 */
@Incubating(since = "5.4")
public interface AttributeMappingsMap {

	void forEachValue(@Nonnull Consumer<? super AttributeMapping> action);

	int size();

	@Nullable
	AttributeMapping get(@Nonnull String name);

	@Nonnull
	Iterable<AttributeMapping> valueIterator();

	@Nonnull
	static Builder builder() {
		return new Builder();
	}

	final class Builder {

		private Builder(){}

		@Nullable private LinkedHashMap<String,AttributeMapping> storage;

		public void put(@Nonnull final String name, @Nonnull final AttributeMapping mapping) {
			Objects.requireNonNull( name );
			Objects.requireNonNull( mapping );
			if ( storage == null ) {
				storage = new LinkedHashMap<>();
			}
			storage.put( name, mapping );
		}

		@Nonnull
		public AttributeMappingsMap build() {
			if ( storage == null ) {
				return EmptyAttributeMappingsMap.INSTANCE;
			}
			else {
				return new ImmutableAttributeMappingsMap( storage );
			}
		}

	}
}
