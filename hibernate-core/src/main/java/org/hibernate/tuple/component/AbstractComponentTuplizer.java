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

import java.lang.reflect.Method;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EmbeddableBinding;
import org.hibernate.property.Getter;
import org.hibernate.property.Setter;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tuple.Instantiator;

/**
 * Support for tuplizers relating to components.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public abstract class AbstractComponentTuplizer implements ComponentTuplizer {
	private final ServiceRegistry serviceRegistry;

	private final Getter[] getters;
	private final Setter[] setters;
	private final int propertySpan;
	private final Instantiator instantiator;
	private final boolean hasCustomAccessors;

	protected AbstractComponentTuplizer(
			ServiceRegistry serviceRegistry,
			EmbeddableBinding embeddableBinding,
			boolean isIdentifierMapper) {
		// TODO: Get rid of the need for isIdentifierMapper arg.
		// Instead the EmbeddedAttributeBinding should be wrapped (e.g., by a proxy)
		// so it can provide the information needed to create getters and setters
		// for an identifier mapper.

		this.serviceRegistry = serviceRegistry;

		propertySpan = embeddableBinding.attributeBindingSpan();
		getters = new Getter[propertySpan];
		setters = new Setter[propertySpan];

		boolean foundCustomAccessor = false;

		int i = 0;
		for ( AttributeBinding attributeBinding : embeddableBinding.attributeBindings() ) {
			getters[i] = buildGetter( embeddableBinding, isIdentifierMapper, attributeBinding );
			setters[i] = buildSetter( embeddableBinding, isIdentifierMapper, attributeBinding );
			if ( !attributeBinding.isBasicPropertyAccessor() ) {
				foundCustomAccessor = true;
			}
			i++;
		}
		hasCustomAccessors = foundCustomAccessor;
		instantiator = buildInstantiator( embeddableBinding, isIdentifierMapper );
	}

	protected ServiceRegistry serviceRegistry() {
		return serviceRegistry;
	}

	protected Getter[] getters() {
		return getters;
	}

	protected Setter[] setters() {
		return setters;
	}

	protected int propertySpan() {
		return propertySpan;
	}

	protected Instantiator instantiator() {
		return instantiator;
	}

	protected boolean hasCustomAccessors() {
		return hasCustomAccessors;
	}

	private ClassLoaderService classLoaderService;

	protected Class classForName(String name) {
		if ( classLoaderService == null ) {
			classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		}

		return classLoaderService.classForName( name );
	}

	protected abstract Instantiator buildInstantiator(
			EmbeddableBinding embeddableBinding,
			boolean isIdentifierMapper
	);

	protected abstract Getter buildGetter(
			EmbeddableBinding embeddableBinding,
			boolean isIdentifierMapper,
			AttributeBinding attributeBinding
	);

	protected abstract Setter buildSetter(
			EmbeddableBinding embeddableBinding,
			boolean isIdentifierMapper,
			AttributeBinding attributeBinding
	);


	public Object getPropertyValue(Object component, int i) throws HibernateException {
		return getters[i].get( component );
	}

	public Object[] getPropertyValues(Object component) throws HibernateException {
		Object[] values = new Object[propertySpan];
		for ( int i = 0; i < propertySpan; i++ ) {
			values[i] = getPropertyValue( component, i );
		}
		return values;
	}

	public boolean isInstance(Object object) {
		return instantiator.isInstance(object);
	}

	public void setPropertyValues(Object component, Object[] values) throws HibernateException {
		for ( int i = 0; i < propertySpan; i++ ) {
			setters[i].set( component, values[i], null );
		}
	}

	/**
	* This method does not populate the component parent
	*/
	public Object instantiate() throws HibernateException {
		return instantiator.instantiate();
	}

	public Object getParent(Object component) {
		return null;
	}

	public boolean hasParentProperty() {
		return false;
	}

	public boolean isMethodOf(Method method) {
		return false;
	}

	public void setParent(Object component, Object parent, SessionFactoryImplementor factory) {
		throw new UnsupportedOperationException();
	}

	public Getter getGetter(int i) {
		return getters[i];
	}
}
