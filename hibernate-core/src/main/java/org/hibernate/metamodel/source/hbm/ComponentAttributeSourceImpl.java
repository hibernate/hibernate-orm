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
package org.hibernate.metamodel.source.hbm;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.EntityMode;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbAnyElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbComponentElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbManyToManyElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbManyToOneElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbOneToManyElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbOneToOneElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbPropertyElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbTuplizerElement;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.source.LocalBindingContext;
import org.hibernate.metamodel.source.binder.AttributeSource;
import org.hibernate.metamodel.source.binder.AttributeSourceContainer;
import org.hibernate.metamodel.source.binder.ComponentAttributeSource;
import org.hibernate.metamodel.source.binder.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.source.binder.MetaAttributeSource;
import org.hibernate.metamodel.source.binder.RelationalValueSource;
import org.hibernate.metamodel.source.binder.SingularAttributeNature;

/**
 * @author Steve Ebersole
 */
public class ComponentAttributeSourceImpl implements ComponentAttributeSource {
	private final JaxbComponentElement componentElement;
	private final AttributeSourceContainer parentContainer;

	private final ValueHolder<Class<?>> componentClassReference;
	private final String path;

	public ComponentAttributeSourceImpl(
			JaxbComponentElement componentElement,
			AttributeSourceContainer parentContainer,
			LocalBindingContext bindingContext) {
		this.componentElement = componentElement;
		this.parentContainer = parentContainer;

		this.componentClassReference = bindingContext.makeClassReference(
				bindingContext.qualifyClassName( componentElement.getClazz() )
		);
		this.path = parentContainer.getPath() + '.' + componentElement.getName();
	}

	@Override
	public String getClassName() {
		return componentElement.getClazz();
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
	public String getParentReferenceAttributeName() {
		return componentElement.getParent() == null ? null : componentElement.getParent().getName();
	}

	@Override
	public String getExplicitTuplizerClassName() {
		if ( componentElement.getTuplizer() == null ) {
			return null;
		}
		final EntityMode entityMode = StringHelper.isEmpty( componentElement.getClazz() ) ? EntityMode.MAP : EntityMode.POJO;
		for ( JaxbTuplizerElement tuplizerElement : componentElement.getTuplizer() ) {
			if ( entityMode == EntityMode.parse( tuplizerElement.getEntityMode() ) ) {
				return tuplizerElement.getClazz();
			}
		}
		return null;
	}

	@Override
	public Iterable<AttributeSource> attributeSources() {
		List<AttributeSource> attributeSources = new ArrayList<AttributeSource>();
		for ( Object attributeElement : componentElement.getPropertyOrManyToOneOrOneToOne() ) {
			if ( JaxbPropertyElement.class.isInstance( attributeElement ) ) {
				attributeSources.add(
						new PropertyAttributeSourceImpl(
								JaxbPropertyElement.class.cast( attributeElement ),
								getLocalBindingContext()
						)
				);
			}
			else if ( JaxbComponentElement.class.isInstance( attributeElement ) ) {
				attributeSources.add(
						new ComponentAttributeSourceImpl(
								(JaxbComponentElement) attributeElement,
								this,
								getLocalBindingContext()
						)
				);
			}
			else if ( JaxbManyToOneElement.class.isInstance( attributeElement ) ) {
				attributeSources.add(
						new ManyToOneAttributeSourceImpl(
								JaxbManyToOneElement.class.cast( attributeElement ),
								getLocalBindingContext()
						)
				);
			}
			else if ( JaxbOneToOneElement.class.isInstance( attributeElement ) ) {
				// todo : implement
			}
			else if ( JaxbAnyElement.class.isInstance( attributeElement ) ) {
				// todo : implement
			}
			else if ( JaxbOneToManyElement.class.isInstance( attributeElement ) ) {
				// todo : implement
			}
			else if ( JaxbManyToManyElement.class.isInstance( attributeElement ) ) {
				// todo : implement
			}
		}
		return attributeSources;
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
		return componentElement.getName();
	}

	@Override
	public boolean isSingular() {
		return true;
	}

	@Override
	public String getPropertyAccessorName() {
		return componentElement.getAccess();
	}

	@Override
	public boolean isInsertable() {
		return componentElement.isInsert();
	}

	@Override
	public boolean isUpdatable() {
		return componentElement.isUpdate();
	}

	@Override
	public PropertyGeneration getGeneration() {
		// todo : is this correct here?
		return null;
	}

	@Override
	public boolean isLazy() {
		return componentElement.isLazy();
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return componentElement.isOptimisticLock();
	}

	@Override
	public Iterable<MetaAttributeSource> metaAttributes() {
		return Helper.buildMetaAttributeSources( componentElement.getMeta() );
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return isInsertable();
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return isUpdatable();
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
