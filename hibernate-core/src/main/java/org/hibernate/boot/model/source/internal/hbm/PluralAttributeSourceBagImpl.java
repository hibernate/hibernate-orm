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
