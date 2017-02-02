/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple.component;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.internal.util.config.ConfigurationHelper;
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
	private final boolean createEmptyCompositesEnabled;

//	public ComponentMetamodel(Component component, SessionFactoryImplementor sessionFactory) {
	public ComponentMetamodel(Component component, MetadataBuildingOptions metadataBuildingOptions) {
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
		final ComponentTuplizerFactory componentTuplizerFactory = new ComponentTuplizerFactory( metadataBuildingOptions );
		final String tuplizerClassName = component.getTuplizerImplClassName( entityMode );
		this.componentTuplizer = tuplizerClassName == null ? componentTuplizerFactory.constructDefaultTuplizer(
				entityMode,
				component
		) : componentTuplizerFactory.constructTuplizer( tuplizerClassName, component );

		final ConfigurationService cs = component.getMetadata().getMetadataBuildingOptions().getServiceRegistry()
				.getService(ConfigurationService.class);

		this.createEmptyCompositesEnabled = ConfigurationHelper.getBoolean(
				Environment.CREATE_EMPTY_COMPOSITES_ENABLED,
				cs.getSettings(),
				false
		);
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

	public boolean isCreateEmptyCompositesEnabled() {
		return createEmptyCompositesEnabled;
	}

}
