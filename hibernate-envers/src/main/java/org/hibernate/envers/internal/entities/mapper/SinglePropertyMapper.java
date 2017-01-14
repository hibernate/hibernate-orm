/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.internal.util.compare.EqualsHelper;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.property.access.spi.SetterFieldImpl;

/**
 * TODO: diff
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 * @author Chris Cranford
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
		if ( (session.getFactory().getJdbcServices()
				.getDialect() instanceof Oracle8iDialect) && (newObj instanceof String || oldObj instanceof String) ) {
			// Don't generate new revision when database replaces empty string with NULL during INSERT or UPDATE statements.
			dbLogicallyDifferent = !(StringTools.isEmpty( newObj ) && StringTools.isEmpty( oldObj ));
		}
		return dbLogicallyDifferent && !EqualsHelper.areEqual( newObj, oldObj );
	}

	@Override
	public void mapModifiedFlagsToMapFromEntity(
			SessionImplementor session,
			Map<String, Object> data,
			Object newObj,
			Object oldObj) {
		// Synthetic properties are not subject to withModifiedFlag analysis
		if ( propertyData.isUsingModifiedFlag() && !propertyData.isSynthetic() ) {
			data.put( propertyData.getModifiedFlagPropertyName(), !EqualsHelper.areEqual( newObj, oldObj ) );
		}
	}

	@Override
	public void mapModifiedFlagsToMapForCollectionChange(String collectionPropertyName, Map<String, Object> data) {
	}

	@Override
	public void mapToEntityFromMap(
			EnversService enversService,
			Object obj,
			Map data,
			Object primaryKey,
			AuditReaderImplementor versionsReader,
			Number revision) {
		// synthetic properties are not part of the entity model; therefore they should be ignored.
		if ( data == null || obj == null || propertyData.isSynthetic() ) {
			return;
		}

		final Setter setter = ReflectionTools.getSetter( obj.getClass(), propertyData, enversService.getServiceRegistry() );
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

		if ( setter instanceof SetterFieldImpl ) {
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

	@Override
	public boolean hasPropertiesWithModifiedFlag() {
		return propertyData != null && propertyData.isUsingModifiedFlag();
	}
}
