/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
