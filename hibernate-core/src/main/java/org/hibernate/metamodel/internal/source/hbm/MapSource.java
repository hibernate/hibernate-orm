/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.internal.source.hbm;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.jaxb.spi.hbm.JaxbIndexElement;
import org.hibernate.jaxb.spi.hbm.JaxbMapElement;
import org.hibernate.jaxb.spi.hbm.JaxbMapKeyElement;
import org.hibernate.metamodel.spi.source.AttributeSourceContainer;
import org.hibernate.metamodel.spi.source.IndexedPluralAttributeSource;

/**
 *
 */
public class MapSource extends AbstractPluralAttributeSourceImpl implements IndexedPluralAttributeSource {

	private final MapKeySourceImpl indexSource;

	/**
	 * @param sourceMappingDocument
	 * @param pluralAttributeElement
	 * @param container
	 */
	public MapSource(
			MappingDocument sourceMappingDocument,
			JaxbMapElement mapElement,
			AttributeSourceContainer container) {
		super( sourceMappingDocument, mapElement, container );
		JaxbMapKeyElement mapKey = mapElement.getMapKey();
		if ( mapKey != null ) {
			this.indexSource = new MapKeySourceImpl( sourceMappingDocument, mapKey );
		}
		else {
			JaxbIndexElement indexElement = mapElement.getIndex();
			if ( indexElement != null ) {
				this.indexSource = new MapKeySourceImpl( sourceMappingDocument, indexElement );
			}
			throw new NotYetImplementedException(
					"<map-key-many-to-many>, <composite-map-key>, <index>, <composite-index>, <index-many-to-many>, and <index-many-to-any>" );
		}
	}

	@Override
	public MapKeySourceImpl getIndexSource() {
		return indexSource;
	}

	@Override
	public JaxbMapElement getPluralAttributeElement() {
		return ( JaxbMapElement ) super.getPluralAttributeElement();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.metamodel.spi.source.PluralAttributeSource#getNature()
	 */
	@Override
	public Nature getNature() {
		return Nature.MAP;
	}
}
