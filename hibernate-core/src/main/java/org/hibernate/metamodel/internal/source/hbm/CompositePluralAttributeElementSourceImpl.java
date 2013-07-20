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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.EntityMode;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.jaxb.spi.hbm.JaxbAnyElement;
import org.hibernate.jaxb.spi.hbm.JaxbCompositeElementElement;
import org.hibernate.jaxb.spi.hbm.JaxbManyToOneElement;
import org.hibernate.jaxb.spi.hbm.JaxbNestedCompositeElementElement;
import org.hibernate.jaxb.spi.hbm.JaxbPropertyElement;
import org.hibernate.jaxb.spi.hbm.JaxbTuplizerElement;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.CompositePluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.LocalBindingContext;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.SingularAttributeSource;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class CompositePluralAttributeElementSourceImpl
		extends AbstractHbmSourceNode
		implements CompositePluralAttributeElementSource {

	private final JaxbCompositeElementElement compositeElement;
	private final Set<CascadeStyle> cascadeStyles;
	private final List<AttributeSource> attributeSources;

	public CompositePluralAttributeElementSourceImpl(
			MappingDocument mappingDocument,
			JaxbCompositeElementElement compositeElement,
			String cascadeString) {
		super( mappingDocument );
		this.compositeElement = compositeElement;
		this.cascadeStyles = Helper.interpretCascadeStyles( cascadeString, bindingContext() );
		this.attributeSources = buildAttributeSources( mappingDocument, compositeElement );
	}

	@Override
	public Nature getNature() {
		return Nature.AGGREGATE;
	}

	@Override
	public String getClassName() {
		return bindingContext().qualifyClassName( compositeElement.getClazz() );
	}

	@Override
	public ValueHolder<Class<?>> getClassReference() {
		return bindingContext().makeClassReference( getClassName() );
	}

	@Override
	public String getParentReferenceAttributeName() {
		return compositeElement.getParent() != null
				? compositeElement.getParent().getName()
				: null;
	}

	@Override
	public String getExplicitTuplizerClassName() {
		if ( compositeElement.getTuplizer() == null ) {
			return null;
		}
		final EntityMode entityMode = StringHelper.isEmpty( compositeElement.getClazz() ) ? EntityMode.MAP : EntityMode.POJO;
		for ( JaxbTuplizerElement tuplizerElement : compositeElement.getTuplizer() ) {
			if ( entityMode == EntityMode.parse( tuplizerElement.getEntityMode().value() ) ) {
				return tuplizerElement.getClazz();
			}
		}
		return null;
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
			JaxbCompositeElementElement compositeElement) {
		List<AttributeSource> attributeSources = new ArrayList<AttributeSource>();
		for( final JaxbAnyElement element : compositeElement.getAny() ) {
			attributeSources.add( buildAttributeSource( mappingDocument, element ) );
		}
		for( final JaxbManyToOneElement element : compositeElement.getManyToOne() ) {
			attributeSources.add( buildAttributeSource( mappingDocument, element ) );
		}
		for( final JaxbNestedCompositeElementElement element : compositeElement.getNestedCompositeElement() ) {
			attributeSources.add( buildAttributeSource( mappingDocument, element ) );
		}
		for( final JaxbPropertyElement element : compositeElement.getProperty() ) {
			attributeSources.add( buildAttributeSource( mappingDocument, element ) );
		}
		return attributeSources;
	}

	@Override
	public LocalBindingContext getLocalBindingContext() {
		return bindingContext();
	}

	@Override
	public Set<CascadeStyle> getCascadeStyles() {
		return cascadeStyles;
	}

	private static AttributeSource buildAttributeSource(
			MappingDocument sourceMappingDocument,
			JaxbAnyElement attributeElement) {
		// todo : implement
		throw new NotYetImplementedException();
	}

	private static SingularAttributeSource buildAttributeSource(
			MappingDocument sourceMappingDocument,
			JaxbPropertyElement attributeElement) {
		return new PropertyAttributeSourceImpl(
				sourceMappingDocument,
				attributeElement,
				null,
				SingularAttributeBinding.NaturalIdMutability.NOT_NATURAL_ID
		);
	}

	private static AttributeSource buildAttributeSource(
			MappingDocument sourceMappingDocument,
			JaxbManyToOneElement attributeElement) {
		return new ManyToOneAttributeSourceImpl(
				sourceMappingDocument,
				JaxbManyToOneElement.class.cast( attributeElement ),
				null,
				SingularAttributeBinding.NaturalIdMutability.NOT_NATURAL_ID
		);
	}

	private static AttributeSource buildAttributeSource(
			MappingDocument sourceMappingDocument,
			JaxbNestedCompositeElementElement attributeElement) {
		// todo : implement
		throw new NotYetImplementedException( "Nested composite element is not supported yet.");
	}

	@Override
	public Iterable<? extends MetaAttributeSource> getMetaAttributeSources() {
		return compositeElement.getMeta();
	}
}
