/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
