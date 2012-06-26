/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.EntityMode;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbComponentElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbTuplizerElement;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.AttributeSourceContainer;
import org.hibernate.metamodel.spi.source.RelationalValueSource;

/**
 * @author Steve Ebersole
 */
class ComponentAttributeSourceImpl extends AbstractComponentAttributeSourceImpl {
	public ComponentAttributeSourceImpl(
			MappingDocument sourceMappingDocument,
			JaxbComponentElement componentElement,
			AttributeSourceContainer parentContainer,
			String logicalTableName,
			SingularAttributeBinding.NaturalIdMutability naturalIdMutability) {
		super( sourceMappingDocument, componentElement, parentContainer, logicalTableName, naturalIdMutability );
	}

	protected JaxbComponentElement componentElement() {
		return (JaxbComponentElement) super.componentSourceElement();
	}

	@Override
	protected List<AttributeSource> buildAttributeSources() {
		List<AttributeSource> attributeSources = new ArrayList<AttributeSource>();
		for ( Object attributeElement : componentElement().getPropertyOrManyToOneOrOneToOne() ) {
			attributeSources.add( buildAttributeSource( attributeElement ) );
		}
		return attributeSources;
	}

	@Override
	public String getParentReferenceAttributeName() {
		return componentElement().getParent() == null ? null : componentElement().getParent().getName();
	}

	@Override
	public String getExplicitTuplizerClassName() {
		if ( componentElement().getTuplizer() == null ) {
			return null;
		}
		final EntityMode entityMode = StringHelper.isEmpty( componentElement().getClazz() ) ? EntityMode.MAP : EntityMode.POJO;
		for ( JaxbTuplizerElement tuplizerElement : componentElement().getTuplizer() ) {
			if ( entityMode == EntityMode.parse( tuplizerElement.getEntityMode() ) ) {
				return tuplizerElement.getClazz();
			}
		}
		return null;
	}

	@Override
	public PropertyGeneration getGeneration() {
		// todo : is this correct here?
		return null;
	}

	@Override
	public boolean isLazy() {
		return componentElement().isLazy();
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return componentElement().isOptimisticLock();
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return componentElement().isInsert();
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return componentElement().isUpdate();
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return true;
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		// none, they are defined on the simple sub-attributes
		return null;
	}
}
