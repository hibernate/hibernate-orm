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

import org.hibernate.AssertionFailure;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmMapType;
import org.hibernate.boot.model.source.spi.AttributeSourceContainer;
import org.hibernate.boot.model.source.spi.PluralAttributeIndexSource;
import org.hibernate.boot.model.source.spi.PluralAttributeNature;

public class PluralAttributeSourceMapImpl extends AbstractPluralAttributeSourceImpl implements IndexedPluralAttributeSource {
	private final PluralAttributeIndexSource indexSource;
	private final String xmlNodeName;

	public PluralAttributeSourceMapImpl(
			MappingDocument sourceMappingDocument,
			JaxbHbmMapType jaxbMap,
			AttributeSourceContainer container) {
		super( sourceMappingDocument, jaxbMap, container );
		this.xmlNodeName = jaxbMap.getNode();

		if (  jaxbMap.getMapKey() != null ) {
			this.indexSource = new PluralAttributeMapKeySourceBasicImpl( sourceMappingDocument,  jaxbMap.getMapKey() );
		}
		else if ( jaxbMap.getIndex() != null ) {
			this.indexSource = new PluralAttributeMapKeySourceBasicImpl( sourceMappingDocument, jaxbMap.getIndex() );
		}
		else if ( jaxbMap.getCompositeMapKey() != null ) {
			this.indexSource = new PluralAttributeMapKeySourceEmbeddedImpl(
					sourceMappingDocument,
					this,
					jaxbMap.getCompositeMapKey()
			);
		}
		else if ( jaxbMap.getCompositeIndex() != null ) {
			this.indexSource = new PluralAttributeMapKeySourceEmbeddedImpl(
					sourceMappingDocument,
					this,
					jaxbMap.getCompositeIndex()
			);
		}
		else if ( jaxbMap.getMapKeyManyToMany() != null ) {
			this.indexSource = new PluralAttributeMapKeyManyToManySourceImpl(
					sourceMappingDocument,
					this,
					jaxbMap.getMapKeyManyToMany()
			);
		}
		else if ( jaxbMap.getIndexManyToMany() != null ) {
			this.indexSource = new PluralAttributeMapKeyManyToManySourceImpl(
					sourceMappingDocument,
					this,
					jaxbMap.getIndexManyToMany()
			);
		}
		else if ( jaxbMap.getIndexManyToAny() != null ) {
			this.indexSource = new PluralAttributeMapKeyManyToAnySourceImpl(
					sourceMappingDocument,
					this,
					jaxbMap.getIndexManyToAny()
			);
		}
		else {
			throw new AssertionFailure( "No map key found" );
		}
	}

	@Override
	public PluralAttributeIndexSource getIndexSource() {
		return indexSource;
	}

	@Override
	public PluralAttributeNature getNature() {
		return PluralAttributeNature.MAP;
	}

	@Override
	public XmlElementMetadata getSourceType() {
		return XmlElementMetadata.MAP;
	}

	@Override
	public String getXmlNodeName() {
		return xmlNodeName;
	}
}
