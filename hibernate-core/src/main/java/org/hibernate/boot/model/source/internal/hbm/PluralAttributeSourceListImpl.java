/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmListType;
import org.hibernate.boot.model.source.spi.AttributeSourceContainer;
import org.hibernate.boot.model.source.spi.PluralAttributeIndexSource;
import org.hibernate.boot.model.source.spi.PluralAttributeNature;
import org.hibernate.boot.model.source.spi.PluralAttributeSequentialIndexSource;

public class PluralAttributeSourceListImpl extends AbstractPluralAttributeSourceImpl implements IndexedPluralAttributeSource {
	private final JaxbHbmListType jaxbListMapping;
	private final PluralAttributeSequentialIndexSource indexSource;

	public PluralAttributeSourceListImpl(
			MappingDocument sourceMappingDocument,
			JaxbHbmListType jaxbListMapping,
			AttributeSourceContainer container) {
		super( sourceMappingDocument, jaxbListMapping, container );
		this.jaxbListMapping = jaxbListMapping;
		if ( jaxbListMapping.getListIndex() != null ) {
			this.indexSource = new PluralAttributeSequentialIndexSourceImpl( sourceMappingDocument(), jaxbListMapping.getListIndex() );
		}
		else {
			this.indexSource = new PluralAttributeSequentialIndexSourceImpl( sourceMappingDocument(), jaxbListMapping.getIndex() );
		}
	}

	@Override
	public PluralAttributeIndexSource getIndexSource() {
		return indexSource;
	}

	@Override
	public PluralAttributeNature getNature() {
		return PluralAttributeNature.LIST;
	}

	@Override
	public XmlElementMetadata getSourceType() {
		return XmlElementMetadata.LIST;
	}

	@Override
	public String getXmlNodeName() {
		return jaxbListMapping.getNode();
	}
}
