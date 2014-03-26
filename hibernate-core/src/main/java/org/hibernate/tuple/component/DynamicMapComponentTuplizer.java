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
import java.util.Map;

import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EmbeddableBinding;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.PropertyAccessorFactory;
import org.hibernate.property.Setter;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tuple.DynamicMapInstantiator;
import org.hibernate.tuple.Instantiator;

/**
 * A {@link ComponentTuplizer} specific to the dynamic-map entity mode.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class DynamicMapComponentTuplizer extends AbstractComponentTuplizer {
	public DynamicMapComponentTuplizer(
			ServiceRegistry serviceRegistry,
			EmbeddableBinding component,
			boolean isIdentifierMapper) {
		super( serviceRegistry, component, isIdentifierMapper );
	}

	@Override
	public Class getMappedClass() {
		return Map.class;
	}

	private PropertyAccessor buildPropertyAccessor() {
		return PropertyAccessorFactory.getDynamicMapPropertyAccessor();
	}

	@Override
	protected Instantiator buildInstantiator(
			EmbeddableBinding embeddableBinding,
			boolean isIdentifierMapper) {
		return new DynamicMapInstantiator();
	}

	@Override
	protected Getter buildGetter(
			EmbeddableBinding embeddableBinding,
			boolean isIdentifierMapper,
			AttributeBinding attributeBinding) {
		return buildPropertyAccessor().getGetter( null, attributeBinding.getAttribute().getName() );
	}

	@Override
	protected Setter buildSetter(
			EmbeddableBinding embeddableBinding,
			boolean isIdentifierMapper,
			AttributeBinding attributeBinding) {
		return buildPropertyAccessor().getSetter( null, attributeBinding.getAttribute().getName() );
	}

}
