/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.internal.binder.Binder;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbCompositeIndexElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbCompositeMapKeyElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbKeyManyToOneElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbKeyPropertyElement;
import org.hibernate.metamodel.source.spi.AttributeSource;
import org.hibernate.metamodel.source.spi.CompositePluralAttributeIndexSource;
import org.hibernate.metamodel.source.spi.HibernateTypeSource;
import org.hibernate.metamodel.source.spi.RelationalValueSource;
import org.hibernate.metamodel.spi.LocalBindingContext;
import org.hibernate.metamodel.spi.binding.PluralAttributeIndexBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;

/**
 * @author Gail Badner
 */
public class CompositePluralAttributeIndexSourceImpl
		extends AbstractHbmSourceNode
		implements CompositePluralAttributeIndexSource {

	private final String className;
	private final List<AttributeSource> attributeSources;

	public CompositePluralAttributeIndexSourceImpl(
			MappingDocument mappingDocument,
			JaxbCompositeIndexElement compositeIndexElement) {
		this(
				mappingDocument,
				compositeIndexElement.getClazz(),
				compositeIndexElement.getKeyProperty(),
				compositeIndexElement.getKeyManyToOne()
		);
	}

	public CompositePluralAttributeIndexSourceImpl(
			MappingDocument mappingDocument,
			JaxbCompositeMapKeyElement compositeMapKeyElement) {
		this(
				mappingDocument,
				compositeMapKeyElement.getClazz(),
				compositeMapKeyElement.getKeyProperty(),
				compositeMapKeyElement.getKeyManyToOne()
		);
	}

	private CompositePluralAttributeIndexSourceImpl(
			MappingDocument mappingDocument,
			String className,
			List<JaxbKeyPropertyElement> keyPropertyElements,
			List<JaxbKeyManyToOneElement> keyManyToOneElements) {
		super( mappingDocument );
		this.className = bindingContext().qualifyClassName( className );
		this.attributeSources = buildAttributeSources(
				mappingDocument,
				keyPropertyElements,
				keyManyToOneElements
		);
	}

	@Override
	public PluralAttributeIndexBinding.Nature getNature() {
		return PluralAttributeIndexBinding.Nature.AGGREGATE;
	}

	@Override
	public List<Binder.DefaultNamingStrategy> getDefaultNamingStrategies() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public HibernateTypeSource getTypeInformation() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean isReferencedEntityAttribute() {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public JavaTypeDescriptor getTypeDescriptor() {
		return bindingContext().typeDescriptor( className );
	}

	@Override
	public String getPath() {
		// todo : implementing this requires passing in the collection source and being able to resolve the collection's role
		return null;
	}

	@Override
	public List<AttributeSource> attributeSources() {
		return attributeSources;
	}

	private static List<AttributeSource> buildAttributeSources(
			MappingDocument mappingDocument,
			List<JaxbKeyPropertyElement> keyPropertyElements,
			List<JaxbKeyManyToOneElement> keyManyToOneElements) {
		List<AttributeSource> attributeSources = new ArrayList<AttributeSource>();
		for ( JaxbKeyPropertyElement keyProperty : keyPropertyElements ){
			attributeSources.add(
					new KeyAttributeSourceImpl(
							mappingDocument,
							keyProperty,
							SingularAttributeBinding.NaturalIdMutability.NOT_NATURAL_ID
					)
			);
		}
		for (JaxbKeyManyToOneElement keyManyToOne :keyManyToOneElements ){
			attributeSources.add(
					new KeyManyToOneSourceImpl(
							mappingDocument,
							keyManyToOne,
							SingularAttributeBinding.NaturalIdMutability.NOT_NATURAL_ID
					)
			);
		}
		return attributeSources;
	}

	@Override
	public LocalBindingContext getLocalBindingContext() {
		return bindingContext();
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		return null;
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return true;
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return false;
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return true;
	}
}
