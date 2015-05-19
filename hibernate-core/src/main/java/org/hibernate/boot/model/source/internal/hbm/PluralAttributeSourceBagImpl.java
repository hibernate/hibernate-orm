/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBagCollectionType;
import org.hibernate.boot.model.source.spi.AttributeSourceContainer;
import org.hibernate.boot.model.source.spi.Orderable;
import org.hibernate.boot.model.source.spi.PluralAttributeNature;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeSourceBagImpl extends AbstractPluralAttributeSourceImpl implements Orderable {
	private final JaxbHbmBagCollectionType jaxbBagMapping;

	public PluralAttributeSourceBagImpl(
			MappingDocument sourceMappingDocument,
			JaxbHbmBagCollectionType jaxbBagMapping,
			AttributeSourceContainer container) {
		super( sourceMappingDocument, jaxbBagMapping, container );
		this.jaxbBagMapping = jaxbBagMapping;
	}

	@Override
	public PluralAttributeNature getNature() {
		return PluralAttributeNature.BAG;
	}

	@Override
	public XmlElementMetadata getSourceType() {
		return XmlElementMetadata.BAG;
	}

	@Override
	public String getXmlNodeName() {
		return jaxbBagMapping.getNode();
	}

	@Override
	public boolean isOrdered() {
		return StringHelper.isNotEmpty( getOrder() );
	}

	@Override
	public String getOrder() {
		return jaxbBagMapping.getOrderBy();
	}
}
