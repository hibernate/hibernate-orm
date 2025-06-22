/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmDiscriminatorSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmEntityBaseDefinition;
import org.hibernate.boot.jaxb.hbm.spi.TableInformationContainer;
import org.hibernate.boot.model.source.spi.EntitySource;
import org.hibernate.boot.model.source.spi.IdentifiableTypeSource;
import org.hibernate.boot.model.source.spi.SubclassEntitySource;
import org.hibernate.boot.model.source.spi.TableSpecificationSource;

import static org.hibernate.boot.model.source.internal.hbm.Helper.createTableSource;

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

		this.primaryTable = entityElement instanceof TableInformationContainer informationContainer
				? createTableSource( sourceMappingDocument(), informationContainer, this )
				: null;

		afterInstantiation();
	}

	@Override
	public TableSpecificationSource getPrimaryTable() {
		return primaryTable;
	}

	@Override
	public String getDiscriminatorMatchValue() {
		return jaxbEntityMapping() instanceof JaxbHbmDiscriminatorSubclassEntityType discriminatorSubclassEntityType
				? discriminatorSubclassEntityType.getDiscriminatorValue()
				: null;
	}

	@Override
	public IdentifiableTypeSource getSuperType() {
		return container;
	}
}
