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
 */
package org.hibernate.envers.entities.mapper.relation.component;

import java.util.Map;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.entities.EntityInstantiator;
import org.hibernate.envers.entities.PropertyData;
import org.hibernate.envers.entities.mapper.CompositeMapperBuilder;
import org.hibernate.envers.entities.mapper.MultiPropertyMapper;
import org.hibernate.envers.entities.mapper.PropertyMapper;
import org.hibernate.envers.entities.mapper.relation.ToOneIdMapper;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.tools.query.Parameters;
import org.hibernate.internal.util.ReflectHelper;

/**
 * @author Kristoffer Lundberg (kristoffer at cambio dot se)
 */
public class MiddleEmbeddableComponentMapper implements MiddleComponentMapper, CompositeMapperBuilder {
	private final MultiPropertyMapper delegate;
	private final Class componentClass;

	public MiddleEmbeddableComponentMapper(MultiPropertyMapper delegate, String componentClassName) {
		this.delegate = delegate;
		try {
			componentClass = Thread.currentThread().getContextClassLoader().loadClass( componentClassName );
		}
		catch ( Exception e ) {
			throw new AuditException( e );
		}
	}

	@Override
	public Object mapToObjectFromFullMap(EntityInstantiator entityInstantiator, Map<String, Object> data, Object dataObject, Number revision) {
		try {
			final Object componentInstance = dataObject != null ? dataObject : ReflectHelper.getDefaultConstructor( componentClass ).newInstance();
			delegate.mapToEntityFromMap(
					entityInstantiator.getAuditConfiguration(), componentInstance, data, null,
					entityInstantiator.getAuditReaderImplementor(), revision
			);
			return componentInstance;
		}
		catch ( Exception e ) {
			throw new AuditException( e );
		}
	}

	@Override
	public void mapToMapFromObject(SessionImplementor session, Map<String, Object> idData, Map<String, Object> data, Object obj) {
		delegate.mapToMapFromEntity( session, data, obj, obj );
	}

	@Override
	public void addMiddleEqualToQuery(Parameters parameters, String idPrefix1, String prefix1, String idPrefix2, String prefix2) {
		addMiddleEqualToQuery( delegate, parameters, idPrefix1, prefix1, idPrefix2, prefix2 );
	}

	protected void addMiddleEqualToQuery(CompositeMapperBuilder compositeMapper, Parameters parameters, String idPrefix1, String prefix1, String idPrefix2, String prefix2) {
		for ( final Map.Entry<PropertyData, PropertyMapper> entry : compositeMapper.getProperties().entrySet() ) {
			final String propertyName = entry.getKey().getName();
			final PropertyMapper nestedMapper = entry.getValue();
			if ( nestedMapper instanceof CompositeMapperBuilder ) {
				addMiddleEqualToQuery( (CompositeMapperBuilder) nestedMapper, parameters, idPrefix1, prefix1, idPrefix2, prefix2 );
			}
			else if ( nestedMapper instanceof ToOneIdMapper ) {
				( (ToOneIdMapper) nestedMapper ).addMiddleEqualToQuery( parameters, idPrefix1, prefix1, idPrefix2, prefix2 );
			}
			else {
				parameters.addWhere( prefix1 + '.' + propertyName, false, "=", prefix2 + '.' + propertyName, false );
			}
		}
	}

	@Override
	public CompositeMapperBuilder addComponent(PropertyData propertyData, String componentClassName) {
		return delegate.addComponent( propertyData, componentClassName );
	}

	@Override
	public void addComposite(PropertyData propertyData, PropertyMapper propertyMapper) {
		delegate.addComposite( propertyData, propertyMapper );
	}

	@Override
	public void add(PropertyData propertyData) {
		delegate.add( propertyData );
	}

	public Map<PropertyData, PropertyMapper> getProperties() {
		return delegate.getProperties();
	}
}
