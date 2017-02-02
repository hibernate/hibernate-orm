/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmArrayType;
import org.hibernate.boot.model.source.spi.AttributeSourceContainer;
import org.hibernate.boot.model.source.spi.PluralAttributeIndexSource;
import org.hibernate.boot.model.source.spi.PluralAttributeNature;
import org.hibernate.boot.model.source.spi.PluralAttributeSequentialIndexSource;
import org.hibernate.boot.model.source.spi.PluralAttributeSourceArray;

/**
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public class PluralAttributeSourceArrayImpl
		extends AbstractPluralAttributeSourceImpl
		implements PluralAttributeSourceArray {
	private final JaxbHbmArrayType jaxbArrayMapping;
	private final PluralAttributeSequentialIndexSource indexSource;

	public PluralAttributeSourceArrayImpl(
			MappingDocument sourceMappingDocument,
			JaxbHbmArrayType jaxbArrayMapping,
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
		return XmlElementMetadata.ARRAY;
	}

	@Override
	public String getXmlNodeName() {
		return jaxbArrayMapping.getNode();
	}

	@Override
	public String getElementClass() {
		return jaxbArrayMapping.getElementClass();
	}
}
