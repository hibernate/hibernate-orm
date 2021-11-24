/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.model;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeIdType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmToolingHintContainer;
import org.hibernate.envers.boot.spi.EnversMetadataBuildingContext;

/**
 * Represents an identifier based on a {@code composite-id} mapping.
 *
 * @author Chris Cranford
 */
public class CompositeIdentifier extends AbstractIdentifier {

	public CompositeIdentifier(EnversMetadataBuildingContext metadataBuildingContext) {
		super( metadataBuildingContext.getConfiguration().getOriginalIdPropertyName() );
	}

	@Override
	public JaxbHbmCompositeIdType build() {
		final JaxbHbmCompositeIdType identifier = new JaxbHbmCompositeIdType();
		identifier.setName( getName() );

		// For all mapped attributes, add them to the list of key properties
		for ( Attribute attribute : getAttributes() ) {
			identifier.getKeyPropertyOrKeyManyToOne().add( (JaxbHbmToolingHintContainer) attribute.build() );
		}

		return identifier;
	}
}
