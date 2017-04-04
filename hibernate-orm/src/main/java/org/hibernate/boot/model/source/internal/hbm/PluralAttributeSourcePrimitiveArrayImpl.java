/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmPrimitiveArrayType;
import org.hibernate.boot.model.source.spi.AttributeSourceContainer;
import org.hibernate.boot.model.source.spi.PluralAttributeIndexSource;
import org.hibernate.boot.model.source.spi.PluralAttributeNature;
import org.hibernate.boot.model.source.spi.PluralAttributeSequentialIndexSource;
import org.hibernate.boot.model.source.spi.PluralAttributeSourceArray;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeSourcePrimitiveArrayImpl
		extends AbstractPluralAttributeSourceImpl
		implements PluralAttributeSourceArray {
	private final PluralAttributeSequentialIndexSource indexSource;
	private final JaxbHbmPrimitiveArrayType jaxbArrayMapping;

	public PluralAttributeSourcePrimitiveArrayImpl(
			MappingDocument sourceMappingDocument,
			JaxbHbmPrimitiveArrayType jaxbArrayMapping,
			AttributeSourceContainer container) {
		super( sourceMappingDocument, jaxbArrayMapping, container );
		this.jaxbArrayMapping = jaxbArrayMapping;
		if ( jaxbArrayMapping.getListIndex() != null ) {
			this.indexSource = new PluralAttributeSequentialIndexSourceImpl( sourceMappingDocument(), jaxbArrayMapping.getListIndex() );
		}
		else {
			this.indexSource = new PluralAttributeSequentialIndexSourceImpl( sourceMappingDocument(), jaxbArrayMapping.getIndex() );
		}
	}

	@Override
	public PluralAttributeIndexSource getIndexSource() {
		return indexSource;
	}

	@Override
	public PluralAttributeNature getNature() {
		return PluralAttributeNature.ARRAY;
	}

	@Override
	public XmlElementMetadata getSourceType() {
		return XmlElementMetadata.PRIMITIVE_ARRAY;
	}

	@Override
	public String getXmlNodeName() {
		return jaxbArrayMapping.getNode();
	}

	@Override
	public String getElementClass() {
		return null;
	}
}
