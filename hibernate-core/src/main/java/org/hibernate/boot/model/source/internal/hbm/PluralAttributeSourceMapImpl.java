/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.AssertionFailure;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmMapType;
import org.hibernate.boot.model.source.spi.AttributeSourceContainer;
import org.hibernate.boot.model.source.spi.PluralAttributeIndexSource;
import org.hibernate.boot.model.source.spi.PluralAttributeNature;
import org.hibernate.boot.model.source.spi.Sortable;
import org.hibernate.internal.util.StringHelper;

public class PluralAttributeSourceMapImpl
		extends AbstractPluralAttributeSourceImpl
		implements IndexedPluralAttributeSource, Sortable {
	private final String sorting;
	private final PluralAttributeIndexSource indexSource;

	private final String xmlNodeName;

	public PluralAttributeSourceMapImpl(
			MappingDocument sourceMappingDocument,
			JaxbHbmMapType jaxbMap,
			AttributeSourceContainer container) {
		super( sourceMappingDocument, jaxbMap, container );
		this.xmlNodeName = jaxbMap.getNode();

		this.sorting = interpretSorting( jaxbMap.getSort() );

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

	private static String interpretSorting(String sort) {
		if ( StringHelper.isEmpty( sort ) ) {
			return null;
		}

		if ( "unsorted".equals( sort ) ) {
			return null;
		}

		return sort;
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

	@Override
	public boolean isSorted() {
		return sorting != null;
	}

	@Override
	public String getComparatorName() {
		return sorting;
	}
}
