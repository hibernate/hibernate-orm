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

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSetType;
import org.hibernate.boot.model.source.spi.AttributeSourceContainer;
import org.hibernate.boot.model.source.spi.Orderable;
import org.hibernate.boot.model.source.spi.PluralAttributeNature;
import org.hibernate.boot.model.source.spi.Sortable;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeSourceSetImpl extends AbstractPluralAttributeSourceImpl implements Orderable, Sortable {
	private final JaxbHbmSetType jaxbSet;

	public PluralAttributeSourceSetImpl(
			MappingDocument sourceMappingDocument,
			JaxbHbmSetType jaxbSet,
			AttributeSourceContainer container) {
		super( sourceMappingDocument, jaxbSet, container );
		this.jaxbSet = jaxbSet;
	}

	@Override
	public PluralAttributeNature getNature() {
		return PluralAttributeNature.SET;
	}

	@Override
	public boolean isSorted() {
		String comparatorName = getComparatorName();
		return StringHelper.isNotEmpty( comparatorName )
				&& !comparatorName.equals( "unsorted" );
	}

	@Override
	public String getComparatorName() {
		return jaxbSet.getSort();
	}

	@Override
	public boolean isOrdered() {
		return StringHelper.isNotEmpty( getOrder() );
	}

	@Override
	public String getOrder() {
		return jaxbSet.getOrderBy();
	}

	@Override
	public XmlElementMetadata getSourceType() {
		return XmlElementMetadata.SET;
	}

	@Override
	public String getXmlNodeName() {
		return jaxbSet.getNode();
	}
}
