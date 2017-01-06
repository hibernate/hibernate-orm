/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation.component;

import java.util.Map;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.EntityInstantiator;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.entities.mapper.CompositeMapperBuilder;
import org.hibernate.envers.internal.entities.mapper.MultiPropertyMapper;
import org.hibernate.envers.internal.entities.mapper.PropertyMapper;
import org.hibernate.envers.internal.entities.mapper.relation.ToOneIdMapper;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.internal.util.ReflectHelper;

/**
 * @author Kristoffer Lundberg (kristoffer at cambio dot se)
 */
public class MiddleEmbeddableComponentMapper implements MiddleComponentMapper, CompositeMapperBuilder {
	private final MultiPropertyMapper delegate;
	private final Class componentClass;

	public MiddleEmbeddableComponentMapper(MultiPropertyMapper delegate, Class componentClass) {
		this.delegate = delegate;
		this.componentClass = componentClass;
	}

	@Override
	public Object mapToObjectFromFullMap(
			EntityInstantiator entityInstantiator,
			Map<String, Object> data,
			Object dataObject,
			Number revision) {
		try {
			final Object componentInstance = dataObject != null
					? dataObject
					: ReflectHelper.getDefaultConstructor( componentClass ).newInstance();
			delegate.mapToEntityFromMap(
					entityInstantiator.getEnversService(),
					componentInstance,
					data,
					null,
					entityInstantiator.getAuditReaderImplementor(),
					revision
			);
			return componentInstance;
		}
		catch (Exception e) {
			throw new AuditException( e );
		}
	}

	@Override
	public void mapToMapFromObject(
			SessionImplementor session,
			Map<String, Object> idData,
			Map<String, Object> data,
			Object obj) {
		delegate.mapToMapFromEntity( session, data, obj, obj );
	}

	@Override
	public void addMiddleEqualToQuery(
			Parameters parameters,
			String idPrefix1,
			String prefix1,
			String idPrefix2,
			String prefix2) {
		addMiddleEqualToQuery( delegate, parameters, idPrefix1, prefix1, idPrefix2, prefix2 );
	}

	protected void addMiddleEqualToQuery(
			CompositeMapperBuilder compositeMapper,
			Parameters parameters,
			String idPrefix1,
			String prefix1,
			String idPrefix2,
			String prefix2) {
		for ( final Map.Entry<PropertyData, PropertyMapper> entry : compositeMapper.getProperties().entrySet() ) {
			final String propertyName = entry.getKey().getName();
			final PropertyMapper nestedMapper = entry.getValue();
			if ( nestedMapper instanceof CompositeMapperBuilder ) {
				addMiddleEqualToQuery(
						(CompositeMapperBuilder) nestedMapper,
						parameters,
						idPrefix1,
						prefix1,
						idPrefix2,
						prefix2
				);
			}
			else if ( nestedMapper instanceof ToOneIdMapper ) {
				( (ToOneIdMapper) nestedMapper ).addMiddleEqualToQuery(
						parameters,
						idPrefix1,
						prefix1,
						idPrefix2,
						prefix2
				);
			}
			else {
				parameters.addWhereOrNullRestriction(
						prefix1 + '.' + propertyName,
						false,
						"=",
						prefix2 + '.' + propertyName, false
				);
			}
		}
	}

	@Override
	public CompositeMapperBuilder addComponent(PropertyData propertyData, Class componentClass) {
		return delegate.addComponent( propertyData, componentClass );
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
