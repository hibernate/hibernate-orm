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
import org.hibernate.internal.jaxb.mapping.hbm.ComponentSourceElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbAnyElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbComponentElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbManyToManyElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbManyToOneElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbOneToManyElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbOneToOneElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbPropertyElement;
import org.hibernate.internal.util.Value;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.AttributeSourceContainer;
import org.hibernate.metamodel.spi.source.ComponentAttributeSource;
import org.hibernate.metamodel.spi.source.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.spi.source.LocalBindingContext;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.SingularAttributeNature;
import org.hibernate.metamodel.spi.source.SingularAttributeSource;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractComponentAttributeSourceImpl extends AbstractHbmSourceNode implements ComponentAttributeSource {
	private final ComponentSourceElement componentSourceElement;
	private final AttributeSourceContainer parentContainer;
	private final List<AttributeSource> subAttributeSources;
	private final SingularAttributeBinding.NaturalIdMutability naturalIdMutability;
	private final Value<Class<?>> componentClassReference;
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

	protected AttributeSource buildAttributeSource(Object attributeElement) {
		if ( JaxbPropertyElement.class.isInstance( attributeElement ) ) {
			return buildPropertyAttributeSource( (JaxbPropertyElement) attributeElement );
		}
		else if ( JaxbComponentElement.class.isInstance( attributeElement ) ) {
			return buildComponentAttributeSource( (JaxbComponentElement) attributeElement );
		}
		else if ( JaxbManyToOneElement.class.isInstance( attributeElement ) ) {
			return buildManyToOneAttributeSource( (JaxbManyToOneElement) attributeElement );
		}
		else if ( JaxbOneToOneElement.class.isInstance( attributeElement ) ) {
			return buildOneToOneAttributeSource( (JaxbOneToOneElement) attributeElement );
		}
		else if ( JaxbAnyElement.class.isInstance( attributeElement ) ) {
			return buildAnyAttributeSource( (JaxbAnyElement) attributeElement );
		}
		else if ( JaxbOneToManyElement.class.isInstance( attributeElement ) ) {
			return buildOneToManyAttributeSource( (JaxbOneToManyElement) attributeElement );
		}
		else if ( JaxbManyToManyElement.class.isInstance( attributeElement ) ) {
			return buildManyToManyAttributeSource( (JaxbManyToManyElement) attributeElement );
		}

		throw new UnexpectedAttributeSourceTypeException(
				"Encountered an unanticipated AttributeSource type : " + attributeElement.getClass().getName()
		);
	}

	protected SingularAttributeSource buildPropertyAttributeSource(JaxbPropertyElement attributeElement) {
		return new PropertyAttributeSourceImpl(
				sourceMappingDocument(),
				attributeElement,
				logicalTableName,
				naturalIdMutability
		);
	}

	protected AttributeSource buildComponentAttributeSource(JaxbComponentElement attributeElement) {
		return new ComponentAttributeSourceImpl(
				sourceMappingDocument(),
				attributeElement,
				this,
				logicalTableName,
				naturalIdMutability
		);
	}

	protected AttributeSource buildManyToOneAttributeSource(JaxbManyToOneElement attributeElement) {
		return new ManyToOneAttributeSourceImpl(
				sourceMappingDocument(),
				JaxbManyToOneElement.class.cast( attributeElement ),
				logicalTableName,
				naturalIdMutability
		);
	}

	protected AttributeSource buildOneToOneAttributeSource(JaxbOneToOneElement attributeElement) {
		// todo : implement
		throw new NotYetImplementedException();
	}

	protected AttributeSource buildAnyAttributeSource(JaxbAnyElement attributeElement) {
		// todo : implement
		throw new NotYetImplementedException();
	}

	protected AttributeSource buildOneToManyAttributeSource(JaxbOneToManyElement attributeElement) {
		// todo : implement
		throw new NotYetImplementedException();
	}

	protected AttributeSource buildManyToManyAttributeSource(JaxbManyToManyElement attributeElement) {
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
	public Value<Class<?>> getClassReference() {
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
	public SingularAttributeNature getNature() {
		return SingularAttributeNature.COMPONENT;
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
	public Iterable<MetaAttributeSource> metaAttributes() {
		return Helper.buildMetaAttributeSources( componentSourceElement.getMeta() );
	}
}
