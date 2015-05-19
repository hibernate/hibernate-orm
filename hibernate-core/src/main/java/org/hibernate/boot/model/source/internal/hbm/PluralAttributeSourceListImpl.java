/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
