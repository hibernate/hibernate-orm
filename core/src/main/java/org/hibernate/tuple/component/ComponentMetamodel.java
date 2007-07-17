package org.hibernate.tuple.component;

import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.HibernateException;
import org.hibernate.tuple.StandardProperty;
import org.hibernate.tuple.PropertyFactory;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

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
	private final ComponentEntityModeToTuplizerMapping tuplizerMapping;

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
			propertyIndexes.put( property.getName(), new Integer( i ) );
			i++;
		}

		tuplizerMapping = new ComponentEntityModeToTuplizerMapping( component );
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

	public ComponentEntityModeToTuplizerMapping getTuplizerMapping() {
		return tuplizerMapping;
	}

}
