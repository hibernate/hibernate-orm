/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.envers.internal.entities.mapper.id;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.internal.util.ReflectHelper;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class MultipleIdMapper extends AbstractCompositeIdMapper implements SimpleIdMapperBuilder {
	public MultipleIdMapper(Class compositeIdClass) {
		super( compositeIdClass );
	}

	@Override
	public void mapToMapFromId(Map<String, Object> data, Object obj) {
		for ( IdMapper idMapper : ids.values() ) {
			idMapper.mapToMapFromEntity( data, obj );
		}
	}

	@Override
	public void mapToMapFromEntity(Map<String, Object> data, Object obj) {
		mapToMapFromId( data, obj );
	}

	@Override
	public boolean mapToEntityFromMap(Object obj, Map data) {
		boolean ret = true;
		for ( IdMapper idMapper : ids.values() ) {
			ret &= idMapper.mapToEntityFromMap( obj, data );
		}

		return ret;
	}

	@Override
	public IdMapper prefixMappedProperties(String prefix) {
		final MultipleIdMapper ret = new MultipleIdMapper( compositeIdClass );

		for ( PropertyData propertyData : ids.keySet() ) {
			final String propertyName = propertyData.getName();
			ret.ids.put( propertyData, new SingleIdMapper( new PropertyData( prefix + propertyName, propertyData ) ) );
		}

		return ret;
	}

	@Override
	public Object mapToIdFromEntity(Object data) {
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
			mapper.mapToEntityFromEntity( ret, data );
		}

		return ret;
	}

	@Override
	public List<QueryParameterData> mapToQueryParametersFromId(Object obj) {
		final Map<String, Object> data = new LinkedHashMap<String, Object>();
		mapToMapFromId( data, obj );

		final List<QueryParameterData> ret = new ArrayList<QueryParameterData>();

		for ( Map.Entry<String, Object> propertyData : data.entrySet() ) {
			ret.add( new QueryParameterData( propertyData.getKey(), propertyData.getValue() ) );
		}

		return ret;
	}
}
