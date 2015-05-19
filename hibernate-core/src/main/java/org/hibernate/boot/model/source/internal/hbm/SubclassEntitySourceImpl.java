/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
