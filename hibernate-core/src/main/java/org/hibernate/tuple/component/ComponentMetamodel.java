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
import java.util.Iterator;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.tuple.PropertyFactory;
import org.hibernate.tuple.StandardProperty;

/**
 * Centralizes metamodel information about a component.
 *
 * @author Steve Ebersole
 */
public class ComponentMetamodel implements Serializable {

	// TODO : will need reference to session factory to fully complete HHH-1907

//	private final SessionFactoryImplementor sessionFactory;
	private final String role;
	private final boolean isKey;
	private final StandardProperty[] properties;

	private final EntityMode entityMode;
	private final ComponentTuplizer componentTuplizer;

	// cached for efficiency...
	private final int propertySpan;
	private final Map propertyIndexes = new HashMap();

//	public ComponentMetamodel(Component component, SessionFactoryImplementor sessionFactory) {
	public ComponentMetamodel(Component component) {
//		this.sessionFactory = sessionFactory;
		this.role = component.getRoleName();
		this.isKey = component.isKey();
		propertySpan = component.getPropertySpan();
		properties = new StandardProperty[propertySpan];
		Iterator itr = component.getPropertyIterator();
		int i = 0;
		while ( itr.hasNext() ) {
			Property property = ( Property ) itr.next();
			properties[i] = PropertyFactory.buildStandardProperty( property, false );
			propertyIndexes.put( property.getName(), i );
			i++;
		}

		entityMode = component.hasPojoRepresentation() ? EntityMode.POJO : EntityMode.MAP;

		// todo : move this to SF per HHH-3517; also see HHH-1907 and ComponentMetamodel
		final ComponentTuplizerFactory componentTuplizerFactory = new ComponentTuplizerFactory();
		final String tuplizerClassName = component.getTuplizerImplClassName( entityMode );
		this.componentTuplizer = tuplizerClassName == null ? componentTuplizerFactory.constructDefaultTuplizer(
				entityMode,
				component
		) : componentTuplizerFactory.constructTuplizer( tuplizerClassName, component );
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
		return index.intValue();
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
