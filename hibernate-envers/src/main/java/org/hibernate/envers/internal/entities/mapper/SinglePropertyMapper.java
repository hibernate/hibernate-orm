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
package org.hibernate.envers.internal.entities.mapper;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.envers.internal.tools.Tools;
import org.hibernate.property.DirectPropertyAccessor;
import org.hibernate.property.Setter;

/**
 * TODO: diff
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class SinglePropertyMapper implements PropertyMapper, SimpleMapperBuilder {
	private PropertyData propertyData;

	public SinglePropertyMapper(PropertyData propertyData) {
		this.propertyData = propertyData;
	}

	public SinglePropertyMapper() {
	}

	@Override
	public void add(PropertyData propertyData) {
		if ( this.propertyData != null ) {
			throw new AuditException( "Only one property can be added!" );
		}

		this.propertyData = propertyData;
	}

	@Override
	public boolean mapToMapFromEntity(
			SessionImplementor session,
			Map<String, Object> data,
			Object newObj,
			Object oldObj) {
		data.put( propertyData.getName(), newObj );
		boolean dbLogicallyDifferent = true;
		if ( (session.getFactory()
				.getDialect() instanceof Oracle8iDialect) && (newObj instanceof String || oldObj instanceof String) ) {
			// Don't generate new revision when database replaces empty string with NULL during INSERT or UPDATE statements.
			dbLogicallyDifferent = !(StringTools.isEmpty( newObj ) && StringTools.isEmpty( oldObj ));
		}
		return dbLogicallyDifferent && !Tools.objectsEqual( newObj, oldObj );
	}

	@Override
	public void mapModifiedFlagsToMapFromEntity(
			SessionImplementor session,
			Map<String, Object> data,
			Object newObj,
			Object oldObj) {
		if ( propertyData.isUsingModifiedFlag() ) {
			data.put( propertyData.getModifiedFlagPropertyName(), !Tools.objectsEqual( newObj, oldObj ) );
		}
	}

	@Override
	public void mapModifiedFlagsToMapForCollectionChange(String collectionPropertyName, Map<String, Object> data) {
	}

	@Override
	public void mapToEntityFromMap(
			AuditConfiguration verCfg, Object obj, Map data, Object primaryKey,
			AuditReaderImplementor versionsReader, Number revision) {
		if ( data == null || obj == null ) {
			return;
		}

		final Setter setter = ReflectionTools.getSetter( obj.getClass(), propertyData );
		final Object value = data.get( propertyData.getName() );
		// We only set a null value if the field is not primite. Otherwise, we leave it intact.
		if ( value != null || !isPrimitive( setter, propertyData, obj.getClass() ) ) {
			setter.set( obj, value, null );
		}
	}

	private boolean isPrimitive(Setter setter, PropertyData propertyData, Class<?> cls) {
		if ( cls == null ) {
			throw new HibernateException( "No field found for property: " + propertyData.getName() );
		}

		if ( setter instanceof DirectPropertyAccessor.DirectSetter ) {
			// In a direct setter, getMethod() returns null
			// Trying to look up the field
			try {
				return cls.getDeclaredField( propertyData.getBeanName() ).getType().isPrimitive();
			}
			catch (NoSuchFieldException e) {
				return isPrimitive( setter, propertyData, cls.getSuperclass() );
			}
		}
		else {
			return setter.getMethod().getParameterTypes()[0].isPrimitive();
		}
	}

	@Override
	public List<PersistentCollectionChangeData> mapCollectionChanges(
			SessionImplementor sessionImplementor,
			String referencingPropertyName,
			PersistentCollection newColl,
			Serializable oldColl,
			Serializable id) {
		return null;
	}

}
