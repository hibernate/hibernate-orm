/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmDiscriminatorSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmEntityBaseDefinition;
import org.hibernate.boot.jaxb.hbm.spi.TableInformationContainer;
import org.hibernate.boot.model.source.spi.EntitySource;
import org.hibernate.boot.model.source.spi.IdentifiableTypeSource;
import org.hibernate.boot.model.source.spi.SubclassEntitySource;
import org.hibernate.boot.model.source.spi.TableSpecificationSource;

/**
 * @author Steve Ebersole
 */
public class SubclassEntitySourceImpl extends AbstractEntitySourceImpl implements SubclassEntitySource {
    private final EntitySource container;
	private final TableSpecificationSource primaryTable;

	protected SubclassEntitySourceImpl(
			MappingDocument sourceMappingDocument,
			JaxbHbmEntityBaseDefinition entityElement,
			EntitySource container) {
		super( sourceMappingDocument, entityElement );
		this.container = container;

		this.primaryTable = TableInformationContainer.class.isInstance( entityElement )
				? Helper.createTableSource( sourceMappingDocument(), (TableInformationContainer) entityElement, this )
				: null;

		afterInstantiation();
	}

	@Override
	public TableSpecificationSource getPrimaryTable() {
		return primaryTable;
	}

	@Override
	public String getDiscriminatorMatchValue() {
		return JaxbHbmDiscriminatorSubclassEntityType.class.isInstance( jaxbEntityMapping() )
				? ( (JaxbHbmDiscriminatorSubclassEntityType) jaxbEntityMapping() ).getDiscriminatorValue()
				: null;
	}

	@Override
	public IdentifiableTypeSource getSuperType() {
		return container;
	}
}
