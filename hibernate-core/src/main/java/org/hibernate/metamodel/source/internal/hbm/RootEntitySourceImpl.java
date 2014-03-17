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
package org.hibernate.metamodel.source.internal.hbm;

import java.util.List;

import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbClassElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbNaturalIdElement;
import org.hibernate.metamodel.source.spi.AttributeSource;
import org.hibernate.metamodel.source.spi.IdentifiableTypeSource;
import org.hibernate.metamodel.source.spi.TableSpecificationSource;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class RootEntitySourceImpl extends AbstractEntitySourceImpl {
	private final TableSpecificationSource primaryTable;

	protected RootEntitySourceImpl(
			MappingDocument sourceMappingDocument,
			JaxbClassElement entityElement) {
		super( sourceMappingDocument, entityElement );
		this.primaryTable = Helper.createTableSource( sourceMappingDocument(), entityElement, this );
		afterInstantiation();
	}

	@Override
	protected List<AttributeSource> buildAttributeSources(List<AttributeSource> attributeSources) {
		final JaxbNaturalIdElement naturalId = entityElement().getNaturalId();
		if ( naturalId != null ) {
			final SingularAttributeBinding.NaturalIdMutability naturalIdMutability = naturalId.isMutable()
					? SingularAttributeBinding.NaturalIdMutability.MUTABLE
					: SingularAttributeBinding.NaturalIdMutability.IMMUTABLE;
			processPropertyAttributes( attributeSources, naturalId.getProperty(), null, naturalIdMutability );
			processManyToOneAttributes( attributeSources, naturalId.getManyToOne(), null, naturalIdMutability );
			processComponentAttributes( attributeSources, naturalId.getComponent(), null, naturalIdMutability );
			processDynamicComponentAttributes(
					attributeSources,
					naturalId.getDynamicComponent(),
					null,
					naturalIdMutability
			);
			processAnyAttributes( attributeSources, naturalId.getAny(), null, naturalIdMutability );
		}
		return super.buildAttributeSources( attributeSources );
	}

	@Override
	protected JaxbClassElement entityElement() {
		return (JaxbClassElement) super.entityElement();
	}

	@Override
	public TableSpecificationSource getPrimaryTable() {
		return primaryTable;
	}

	@Override
	public String getDiscriminatorMatchValue() {
		return entityElement().getDiscriminatorValue();
	}

	@Override
	public IdentifiableTypeSource getSuperType() {
		return null;
	}
}
