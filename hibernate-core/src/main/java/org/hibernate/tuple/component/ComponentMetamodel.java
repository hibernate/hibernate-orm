/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.tuple.component;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EmbeddableBinding;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tuple.PropertyFactory;
import org.hibernate.tuple.StandardProperty;

/**
 * Centralizes metamodel information about a component.
 *
 * @author Steve Ebersole
 */
public class ComponentMetamodel implements Serializable {
	private final String role;
	private final boolean isKey;
	private final StandardProperty[] properties;

	private final EntityMode entityMode;
	private final ComponentTuplizer componentTuplizer;

	// cached for efficiency...
	private final int propertySpan;
	private final Map propertyIndexes = new HashMap();

	public ComponentMetamodel(
			ServiceRegistry serviceRegistry,
			EmbeddableBinding component,
			boolean isIdentifierAttributeBinding,
			boolean isIdentifierMapper) {
		this.isKey = isIdentifierAttributeBinding;
		this.role = component.getPathBase().getFullPath();
		propertySpan = component.attributeBindingSpan();
		properties = new StandardProperty[propertySpan];
		int i = 0;
		for ( AttributeBinding attributeBinding : component.attributeBindings() ) {
			properties[i] = PropertyFactory.buildStandardProperty( attributeBinding, false );
			propertyIndexes.put( attributeBinding.getAttribute().getName(), i );
			i++;
		}

		entityMode = component.seekEntityBinding().getHierarchyDetails().getEntityMode();

		// todo : move this to SF per HHH-3517; also see HHH-1907 and ComponentMetamodel
		final ComponentTuplizerFactory componentTuplizerFactory = new ComponentTuplizerFactory();
		final Class<? extends ComponentTuplizer> tuplizerClass = component.getCustomTuplizerClass();
		if ( tuplizerClass == null ) {
			componentTuplizer = componentTuplizerFactory.constructDefaultTuplizer(
					entityMode, serviceRegistry, component, isIdentifierMapper
			);
		}
		else {
			componentTuplizer = componentTuplizerFactory.constructTuplizer(
					tuplizerClass, serviceRegistry, component, isIdentifierMapper
			);
		}
	}

	public boolean isKey() {
		return isKey;
	}

	public int getPropertySpan() {
		return propertySpan;
	}

	public StandardProperty[] getProperties() {
		return properties;
	}

	public StandardProperty getProperty(int index) {
		if ( index < 0 || index >= propertySpan ) {
			throw new IllegalArgumentException( "illegal index value for component property access [request=" + index + ", span=" + propertySpan + "]" );
		}
		return properties[index];
	}

	public int getPropertyIndex(String propertyName) {
		Integer index = ( Integer ) propertyIndexes.get( propertyName );
		if ( index == null ) {
			throw new HibernateException( "component does not contain such a property [" + propertyName + "]" );
		}
		return index;
	}

	public StandardProperty getProperty(String propertyName) {
		return getProperty( getPropertyIndex( propertyName ) );
	}

	public EntityMode getEntityMode() {
		return entityMode;
	}

	public ComponentTuplizer getComponentTuplizer() {
		return componentTuplizer;
	}

}
