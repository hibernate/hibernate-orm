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

import java.util.Collection;
import java.util.List;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.internal.jaxb.hbm.ComponentSourceElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbAnyElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbArrayElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbBagElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbComponentElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbDynamicComponentElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbListElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbManyToManyElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbManyToOneElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbMapElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbOneToManyElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbOneToOneElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbPrimitiveArrayElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbPropertyElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbSetElement;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.AttributeSourceContainer;
import org.hibernate.metamodel.spi.source.ComponentAttributeSource;
import org.hibernate.metamodel.spi.source.HibernateTypeSource;
import org.hibernate.metamodel.spi.LocalBindingContext;
import org.hibernate.metamodel.spi.source.ToolingHintSource;
import org.hibernate.metamodel.spi.source.SingularAttributeSource;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractComponentAttributeSourceImpl extends AbstractHbmSourceNode implements ComponentAttributeSource {
	private final ComponentSourceElement componentSourceElement;
	private final AttributeSourceContainer parentContainer;
	private final List<AttributeSource> subAttributeSources;
	private final SingularAttributeBinding.NaturalIdMutability naturalIdMutability;
	private final JavaTypeDescriptor componentTypeDescriptor;
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
		this.componentTypeDescriptor = typeDescriptor( componentSourceElement.getClazz() );
		this.logicalTableName = logicalTableName;
		this.path = parentContainer.getPath() + '.' + componentSourceElement.getName();
		
		this.subAttributeSources = buildAttributeSources();
	}

	@Override
	public String getContainingTableName() {
		return logicalTableName;
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
				attributeElement,
				logicalTableName,
				naturalIdMutability
		);
	}

	protected AttributeSource buildAttributeSource(JaxbOneToOneElement attributeElement) {
		return new OneToOneAttributeSourceImpl(
				sourceMappingDocument(),
				attributeElement,
				logicalTableName,
				naturalIdMutability
		);
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
		return new MapSourceImpl(
				sourceMappingDocument(),
				attributeElement,
				parentContainer
		);
	}
	protected AttributeSource buildAttributeSource(JaxbSetElement attributeElement) {
		return new SetSourceImpl(
				sourceMappingDocument(),
				attributeElement,
				parentContainer
		);
	}
	protected AttributeSource buildAttributeSource(JaxbListElement attributeElement) {
		return new ListSourceImpl(
				sourceMappingDocument(),
				attributeElement,
				parentContainer
		);
	}
	protected AttributeSource buildAttributeSource(JaxbBagElement attributeElement) {
		return new BagSourceImpl(
				sourceMappingDocument(),
				attributeElement,
				parentContainer
		);
	}
	protected AttributeSource buildAttributeSource(JaxbArrayElement attributeElement) {
		return new ArraySourceImpl(
				sourceMappingDocument(),
				attributeElement,
				parentContainer
		);
	}
	protected AttributeSource buildAttributeSource(JaxbPrimitiveArrayElement attributeElement) {
		// todo : implement
		throw new NotYetImplementedException();
	}


	protected ComponentSourceElement componentSourceElement() {
		return componentSourceElement;
	}

	@Override
	public JavaTypeDescriptor getTypeDescriptor() {
		return componentTypeDescriptor;
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
	public HibernateTypeSource getTypeInformation() {
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
	public Collection<? extends ToolingHintSource> getToolingHintSources() {
		return componentSourceElement.getMeta();
	}
}
