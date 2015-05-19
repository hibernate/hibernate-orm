/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
