/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.property.access.spi.SetterFieldImpl;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * TODO: diff
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 * @author Chris Cranford
 */
public class SinglePropertyMapper extends AbstractPropertyMapper implements SimpleMapperBuilder {
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
			SharedSessionContractImplementor session,
			Map<String, Object> data,
			Object newObj,
			Object oldObj) {
		data.put( propertyData.getName(), newObj );
		boolean dbLogicallyDifferent = true;
		final Dialect dialect = session.getFactory().getJdbcServices().getDialect();
		if ( ( dialect instanceof OracleDialect ) && (newObj instanceof String || oldObj instanceof String) ) {
			// Don't generate new revision when database replaces empty string with NULL during INSERT or UPDATE statements.
			dbLogicallyDifferent = !(StringTools.isEmpty( newObj ) && StringTools.isEmpty( oldObj ));
		}
		return dbLogicallyDifferent && !areEqual( newObj, oldObj );
	}

	@Override
	public void mapModifiedFlagsToMapFromEntity(
			SharedSessionContractImplementor session,
			Map<String, Object> data,
			Object newObj,
			Object oldObj) {
		// Synthetic properties are not subject to withModifiedFlag analysis
		if ( propertyData.isUsingModifiedFlag() && !propertyData.isSynthetic() ) {
			data.put( propertyData.getModifiedFlagPropertyName(), !areEqual( newObj, oldObj ) );
		}
	}

	@Override
	public void mapModifiedFlagsToMapForCollectionChange(String collectionPropertyName, Map<String, Object> data) {
	}

	@Override
	public void mapToEntityFromMap(
			final EnversService enversService,
			final Object obj,
			final Map data,
			Object primaryKey,
			AuditReaderImplementor versionsReader,
			Number revision) {
		// synthetic properties are not part of the entity model; therefore they should be ignored.
		if ( data == null || obj == null || propertyData.isSynthetic() ) {
			return;
		}

		final Object value = data.get( propertyData.getName() );

		if ( isDynamicComponentMap() ) {
			@SuppressWarnings("unchecked")
			final Map<String, Object> map = (Map<String, Object>) obj;
			map.put( propertyData.getBeanName(), value );
		}
		else {
			final Setter setter = ReflectionTools.getSetter(
					obj.getClass(),
					propertyData,
					enversService.getServiceRegistry()
			);

			// We only set a null value if the field is not primitive. Otherwise, we leave it intact.
			if ( value != null || !isPrimitive( setter, propertyData, obj.getClass() ) ) {
				setter.set( obj, value );
			}
		}
	}

	@Override
	public Object mapToEntityFromMap(
			final EnversService enversService,
			final Map data,
			Object primaryKey,
			AuditReaderImplementor versionsReader,
			Number revision) {
		// synthetic properties are not part of the entity model; therefore they should be ignored.
		if ( data == null || propertyData.isSynthetic() ) {
			return null;
		}

		return data.get( propertyData.getName() );
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
			SharedSessionContractImplementor sessionImplementor,
			String referencingPropertyName,
			PersistentCollection newColl,
			Serializable oldColl,
			Object id) {
		return null;
	}

	@Override
	public boolean hasPropertiesWithModifiedFlag() {
		return propertyData != null && propertyData.isUsingModifiedFlag();
	}

	private boolean areEqual(Object newObj, Object oldObj) {
		// Should a Type have been specified on the property mapper, delegate there to make sure
		// that proper equality comparison occurs based on the Type's semantics rather than the
		// generalized EqualsHelper #areEqual call.
		if ( propertyData.getType() != null ) {
			return propertyData.getType().isEqual( newObj, oldObj );
		}
		// todo (6.0) - Confirm if this is still necessary as everything should use a JavaTypeDescriptor.
		//		This was maintained for legacy 5.2 behavior only.
		return Objects.deepEquals( newObj, oldObj );
	}
}
