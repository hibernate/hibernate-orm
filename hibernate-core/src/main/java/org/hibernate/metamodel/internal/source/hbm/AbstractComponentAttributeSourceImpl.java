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

import java.util.List;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.jaxb.spi.hbm.ComponentSourceElement;
import org.hibernate.jaxb.spi.hbm.JaxbAnyElement;
import org.hibernate.jaxb.spi.hbm.JaxbArrayElement;
import org.hibernate.jaxb.spi.hbm.JaxbBagElement;
import org.hibernate.jaxb.spi.hbm.JaxbComponentElement;
import org.hibernate.jaxb.spi.hbm.JaxbDynamicComponentElement;
import org.hibernate.jaxb.spi.hbm.JaxbListElement;
import org.hibernate.jaxb.spi.hbm.JaxbManyToManyElement;
import org.hibernate.jaxb.spi.hbm.JaxbManyToOneElement;
import org.hibernate.jaxb.spi.hbm.JaxbMapElement;
import org.hibernate.jaxb.spi.hbm.JaxbOneToManyElement;
import org.hibernate.jaxb.spi.hbm.JaxbOneToOneElement;
import org.hibernate.jaxb.spi.hbm.JaxbPrimitiveArrayElement;
import org.hibernate.jaxb.spi.hbm.JaxbPropertyElement;
import org.hibernate.jaxb.spi.hbm.JaxbSetElement;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.AttributeSourceContainer;
import org.hibernate.metamodel.spi.source.ComponentAttributeSource;
import org.hibernate.metamodel.spi.source.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.spi.source.LocalBindingContext;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.SingularAttributeSource;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractComponentAttributeSourceImpl extends AbstractHbmSourceNode implements ComponentAttributeSource {
	private final ComponentSourceElement componentSourceElement;
	private final AttributeSourceContainer parentContainer;
	private final List<AttributeSource> subAttributeSources;
	private final SingularAttributeBinding.NaturalIdMutability naturalIdMutability;
	private final ValueHolder<Class<?>> componentClassReference;
	private final String logicalTableName;
	private final String path;

	protected AbstractComponentAttributeSourceImpl(
			MappingDocument sourceMappingDocument,
			ComponentSourceElement componentSourceElement,
			AttributeSourceContainer parentContainer,
			String logicalTableName,
			SingularAttributeBinding.NaturalIdMutability naturalIdMutability) {
		super( sourceMappingDocument );
		this.componentSourceElement = componentSourceElement;
		this.parentContainer = parentContainer;
		this.naturalIdMutability = naturalIdMutability;
		this.componentClassReference = makeClassReference( componentSourceElement.getClazz() );
		this.logicalTableName = logicalTableName;
		this.path = parentContainer.getPath() + '.' + componentSourceElement.getName();

		this.subAttributeSources = buildAttributeSources();
	}

	protected abstract List<AttributeSource> buildAttributeSources();
	protected SingularAttributeSource buildAttributeSource(JaxbPropertyElement attributeElement) {
		return new PropertyAttributeSourceImpl(
				sourceMappingDocument(),
				attributeElement,
				logicalTableName,
				naturalIdMutability
		);
	}

	protected AttributeSource buildAttributeSource(JaxbComponentElement attributeElement) {
		return new ComponentAttributeSourceImpl(
				sourceMappingDocument(),
				attributeElement,
				this,
				logicalTableName,
				naturalIdMutability
		);
	}
	protected AttributeSource buildAttributeSource(JaxbDynamicComponentElement attributeElement){
		// todo : implement
		throw new NotYetImplementedException();
	}
	protected AttributeSource buildAttributeSource(JaxbManyToOneElement attributeElement) {
		return new ManyToOneAttributeSourceImpl(
				sourceMappingDocument(),
				JaxbManyToOneElement.class.cast( attributeElement ),
				logicalTableName,
				naturalIdMutability
		);
	}

	protected AttributeSource buildAttributeSource(JaxbOneToOneElement attributeElement) {
		// todo : implement
		throw new NotYetImplementedException();
	}

	protected AttributeSource buildAttributeSource(JaxbAnyElement attributeElement) {
		// todo : implement
		throw new NotYetImplementedException();
	}

	protected AttributeSource buildAttributeSource(JaxbOneToManyElement attributeElement) {
		// todo : implement
		throw new NotYetImplementedException();
	}

	protected AttributeSource buildAttributeSource(JaxbManyToManyElement attributeElement) {
		// todo : implement
		throw new NotYetImplementedException();
	}
	// todo duplicated with org.hibernate.metamodel.internal.source.hbm.AbstractEntitySourceImpl
	protected AttributeSource buildAttributeSource(JaxbMapElement attributeElement){
		// todo : implement
		throw new NotYetImplementedException();
	}
	protected AttributeSource buildAttributeSource(JaxbSetElement attributeElement) {
		// todo : implement
		throw new NotYetImplementedException();
	}
	protected AttributeSource buildAttributeSource(JaxbListElement attributeElement) {
		// todo : implement
		throw new NotYetImplementedException();
	}
	protected AttributeSource buildAttributeSource(JaxbBagElement attributeElement) {
		// todo : implement
		throw new NotYetImplementedException();
	}
	protected AttributeSource buildAttributeSource(JaxbArrayElement attributeElement) {
		// todo : implement
		throw new NotYetImplementedException();
	}
	protected AttributeSource buildAttributeSource(JaxbPrimitiveArrayElement attributeElement) {
		// todo : implement
		throw new NotYetImplementedException();
	}


	protected ComponentSourceElement componentSourceElement() {
		return componentSourceElement;
	}

	@Override
	public String getClassName() {
		return componentSourceElement.getClazz();
	}

	@Override
	public ValueHolder<Class<?>> getClassReference() {
		return componentClassReference;
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public LocalBindingContext getLocalBindingContext() {
		return parentContainer.getLocalBindingContext();
	}

	@Override
	public List<AttributeSource> attributeSources() {
		return subAttributeSources;
	}

	@Override
	public boolean isVirtualAttribute() {
		return false;
	}

	@Override
	public Nature getNature() {
		return Nature.COMPOSITE;
	}

	@Override
	public ExplicitHibernateTypeSource getTypeInformation() {
		// <component/> does not support type information.
		return null;
	}

	@Override
	public String getName() {
		return componentSourceElement.getName();
	}

	@Override
	public boolean isSingular() {
		return true;
	}

	@Override
	public String getPropertyAccessorName() {
		return componentSourceElement.getAccess();
	}

	@Override
	public SingularAttributeBinding.NaturalIdMutability getNaturalIdMutability() {
		return naturalIdMutability;
	}

	@Override
	public Iterable<? extends MetaAttributeSource> getMetaAttributeSources() {
		return componentSourceElement.getMeta();
	}
}
