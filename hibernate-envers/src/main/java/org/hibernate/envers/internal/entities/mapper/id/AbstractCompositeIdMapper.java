/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.id;

import java.util.Map;

import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.tools.Tools;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public abstract class AbstractCompositeIdMapper extends AbstractIdMapper implements SimpleIdMapperBuilder {
	protected final Class compositeIdClass;

	protected Map<PropertyData, SingleIdMapper> ids;

	protected AbstractCompositeIdMapper(Class compositeIdClass, ServiceRegistry serviceRegistry) {
		super( serviceRegistry );
		this.compositeIdClass = compositeIdClass;
		ids = Tools.newLinkedHashMap();
	}

	@Override
	public void add(PropertyData propertyData) {
		ids.put( propertyData, new SingleIdMapper( getServiceRegistry(), propertyData ) );
	}

	@Override
	public Object mapToIdFromMap(Map data) {
		if ( data == null ) {
			return null;
		}

		final Object ret;
		try {
			ret = ReflectHelper.getDefaultConstructor( compositeIdClass ).newInstance();
		}
		catch (Exception e) {
			throw new AuditException( e );
		}

		for ( SingleIdMapper mapper : ids.values() ) {
			if ( !mapper.mapToEntityFromMap( ret, data ) ) {
				return null;
			}
		}

		return ret;
	}
}
