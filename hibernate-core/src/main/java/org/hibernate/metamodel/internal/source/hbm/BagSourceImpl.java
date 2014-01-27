/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.hbm;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.jaxb.spi.hbm.JaxbBagElement;
import org.hibernate.metamodel.spi.source.AttributeSourceContainer;
import org.hibernate.metamodel.spi.source.Orderable;

/**
 * @author Steve Ebersole
 */
public class BagSourceImpl extends AbstractPluralAttributeSourceImpl implements Orderable {
	public BagSourceImpl(
			MappingDocument sourceMappingDocument,
			JaxbBagElement bagElement,
			AttributeSourceContainer container) {
		super( sourceMappingDocument, bagElement, container );
	}

	@Override
	public Nature getNature() {
		return Nature.BAG;
	}

	@Override
	public JaxbBagElement getPluralAttributeElement() {
		return (JaxbBagElement) super.getPluralAttributeElement();
	}

	@Override
	public boolean isOrdered() {
		return StringHelper.isNotEmpty( getOrder() );
	}

	@Override
	public String getOrder() {
		return getPluralAttributeElement().getOrderBy();
	}
}
