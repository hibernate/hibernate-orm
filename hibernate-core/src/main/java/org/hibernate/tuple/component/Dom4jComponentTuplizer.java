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

import org.dom4j.Element;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.PropertyAccessorFactory;
import org.hibernate.property.Setter;
import org.hibernate.tuple.Instantiator;
import org.hibernate.tuple.Dom4jInstantiator;

/**
 * A {@link ComponentTuplizer} specific to the dom4j entity mode.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class Dom4jComponentTuplizer extends AbstractComponentTuplizer  {

	public Class getMappedClass() {
		return Element.class;
	}

	public Dom4jComponentTuplizer(Component component) {
		super(component);
	}

	protected Instantiator buildInstantiator(Component component) {
		return new Dom4jInstantiator( component );
	}

	private PropertyAccessor buildPropertyAccessor(Property property) {
		//TODO: currently we don't know a SessionFactory reference when building the Tuplizer
		//      THIS IS A BUG (embedded-xml=false on component)
		// TODO : fix this after HHH-1907 is complete
		return PropertyAccessorFactory.getDom4jPropertyAccessor( property.getNodeName(), property.getType(), null );
	}

	protected Getter buildGetter(Component component, Property prop) {
		return buildPropertyAccessor(prop).getGetter( null, prop.getName() );
	}

	protected Setter buildSetter(Component component, Property prop) {
		return buildPropertyAccessor(prop).getSetter( null, prop.getName() );
	}

}
