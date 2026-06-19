/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.materialize;

import org.hibernate.AnnotationException;
import org.hibernate.boot.models.mapping.internal.view.CollationContributionView;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.Property;

/// Materializes the legacy column collation for a `@Collate` attribute.
///
/// `@Collate` is a compact proof for value/selectable-oriented attribute
/// contributions.  The binding contribution owns the requested collation; this
/// materializer applies it to the columns exposed by the legacy mapping value.
///
/// @since 9.0
/// @author Steve Ebersole
public class CollationMappingMaterializer {
	public void materializeCollation(CollationContributionView contribution, Property property) {
		final var value = property.getValue();
		if ( value instanceof OneToMany ) {
			throw new AnnotationException( "One to many association '" + property.getName()
					+ "' was annotated '@Collate'" );
		}
		else if ( value instanceof Collection ) {
			throw new AnnotationException( "Collection '" + property.getName()
					+ "' was annotated '@Collate'" );
		}
		else {
			for ( Column column : value.getColumns() ) {
				column.setCollation( contribution.collation() );
			}
		}
	}
}
