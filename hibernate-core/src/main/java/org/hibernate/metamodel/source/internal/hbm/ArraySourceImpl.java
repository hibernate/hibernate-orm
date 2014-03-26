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
package org.hibernate.metamodel.source.internal.hbm;

import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbArrayElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbListIndexElement;
import org.hibernate.metamodel.source.spi.AttributeSourceContainer;
import org.hibernate.metamodel.source.spi.IndexedPluralAttributeSource;
import org.hibernate.metamodel.source.spi.PluralAttributeIndexSource;
import org.hibernate.metamodel.source.spi.PluralAttributeSequentialIndexSource;
import org.hibernate.metamodel.spi.PluralAttributeNature;

/**
 * @author Brett Meyer
 */
public class ArraySourceImpl extends AbstractPluralAttributeSourceImpl implements IndexedPluralAttributeSource {

	private final PluralAttributeSequentialIndexSource indexSource;

	public ArraySourceImpl(
			MappingDocument sourceMappingDocument,
			JaxbArrayElement arrayElement,
			AttributeSourceContainer container) {
		super( sourceMappingDocument, arrayElement, container );
		JaxbListIndexElement listIndexElement = arrayElement.getListIndex();
		if ( listIndexElement == null ) {
			this.indexSource = new PluralAttributeSequentialIndexSourceImpl( sourceMappingDocument(), arrayElement.getIndex() );
		} else {
			this.indexSource = new PluralAttributeSequentialIndexSourceImpl( sourceMappingDocument(), listIndexElement );
		}
	}

	@Override
	public PluralAttributeIndexSource getIndexSource() {
		return indexSource;
	}

	@Override
	public JaxbArrayElement getPluralAttributeElement() {
		return ( JaxbArrayElement ) super.getPluralAttributeElement();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.metamodel.source.spi.PluralAttributeSource#getNature()
	 */
	@Override
	public PluralAttributeNature getNature() {
		return PluralAttributeNature.ARRAY;
	}
}
