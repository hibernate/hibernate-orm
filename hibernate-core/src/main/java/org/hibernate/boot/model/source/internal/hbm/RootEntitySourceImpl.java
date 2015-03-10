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

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNaturalIdType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmRootEntityType;
import org.hibernate.boot.model.source.spi.IdentifiableTypeSource;
import org.hibernate.boot.model.source.spi.NaturalIdMutability;
import org.hibernate.boot.model.source.spi.TableSpecificationSource;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class RootEntitySourceImpl extends AbstractEntitySourceImpl {
	private final TableSpecificationSource primaryTable;

	protected RootEntitySourceImpl(
			MappingDocument sourceMappingDocument,
			JaxbHbmRootEntityType entityElement) {
		super( sourceMappingDocument, entityElement );
		this.primaryTable = Helper.createTableSource(
				sourceMappingDocument(),
				entityElement,
				this,
				entityElement.getRowid(),
				entityElement.getComment(),
				entityElement.getCheck()
		);
		afterInstantiation();
	}

	@Override
	protected void buildAttributeSources(final AttributesHelper.Callback attributeBuildingCallback) {
		final JaxbHbmNaturalIdType naturalId = jaxbEntityMapping().getNaturalId();
		if ( naturalId != null ) {
			final NaturalIdMutability naturalIdMutability = naturalId.isMutable()
					? NaturalIdMutability.MUTABLE
					: NaturalIdMutability.IMMUTABLE;

			AttributesHelper.processAttributes(
					sourceMappingDocument(),
					attributeBuildingCallback,
					naturalId.getAttributes(),
					null,
					naturalIdMutability
			);
		}

		super.buildAttributeSources( attributeBuildingCallback );
	}

	@Override
	protected JaxbHbmRootEntityType jaxbEntityMapping() {
		return (JaxbHbmRootEntityType) super.jaxbEntityMapping();
	}

	@Override
	public TableSpecificationSource getPrimaryTable() {
		return primaryTable;
	}

	@Override
	public String getDiscriminatorMatchValue() {
		return jaxbEntityMapping().getDiscriminatorValue();
	}

	@Override
	public IdentifiableTypeSource getSuperType() {
		return null;
	}
}
