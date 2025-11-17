/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.model;

import java.util.Iterator;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmEntityDiscriminatorType;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Value;

/**
 * Contract for a persistent entity discriminator type.
 *
 * @author Chris Cranford
 */
public class DiscriminatorType implements Bindable<JaxbHbmEntityDiscriminatorType> {

	private final Value discriminator;

	public DiscriminatorType(Value discriminator) {
		this.discriminator = discriminator;
	}

	@Override
	public JaxbHbmEntityDiscriminatorType build() {
		final JaxbHbmEntityDiscriminatorType mapping = new JaxbHbmEntityDiscriminatorType();
		mapping.setType( discriminator.getType().getName() );

		final Iterator<Selectable> iterator = discriminator.getSelectables().iterator();
		while ( iterator.hasNext() ) {
			final Selectable selectable = iterator.next();
			if ( selectable.isFormula() ) {
				mapping.setFormula( Formula.from( selectable ).build() );
			}
			else {
				mapping.setColumn( Column.from( selectable ).build() );
			}
		}

		return mapping;
	}
}
