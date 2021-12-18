/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.id;

import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.mapping.Component;

/**
 * An identifier mapper that is meant to support nested {@link jakarta.persistence.Embeddable} instances
 * inside an existing {@link jakarta.persistence.EmbeddedId} identifier hierarchy.
 *
 * @author Chris Cranford
 */
public class NestedEmbeddedIdMapper extends EmbeddedIdMapper {
	public NestedEmbeddedIdMapper(PropertyData propertyData, Component component) {
		super( propertyData, component );
	}
}
