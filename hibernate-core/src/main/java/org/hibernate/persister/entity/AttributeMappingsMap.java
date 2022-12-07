/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.entity;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.Consumer;

import org.hibernate.Incubating;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.persister.internal.EmptyAttributeMappingsMap;

@Incubating
public interface AttributeMappingsMap {

	void forEachValue(Consumer<? super AttributeMapping> action);

	int size();

	AttributeMapping get(String name);

	Iterable<AttributeMapping> valueIterator();

	static Builder builder() {
		return new Builder();
	}

	final class Builder {

		private Builder(){}

		//Intentionally using raw types
		private LinkedHashMap storage;

		public void put(final String name, final AttributeMapping mapping) {
			Objects.requireNonNull( name );
			Objects.requireNonNull( mapping );
			if ( storage == null ) {
				storage = new LinkedHashMap();
			}
			storage.put( name, mapping );
		}

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
