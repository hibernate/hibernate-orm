/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper;

import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.property.access.spi.SetterFieldImpl;

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
			SessionImplementor session,
			Map<String, Object> data,
			Object newObj,
			Object oldObj) {
		data.put( propertyData.getName(), newObj );

		if ( newObj instanceof String || oldObj instanceof String ) {
			if ( session.getFactory().getJdbcServices().getDialect().isEmptyStringTreatedAsNull() ) {
				if ( StringTools.isEmpty( newObj ) && StringTools.isEmpty( oldObj ) ) {
					return false;
				}
			}
		}

		return !areEqual( newObj, oldObj );
	}

	@Override
	public void mapModifiedFlagsToMapFromEntity(
			SessionImplementor session,
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
			Object obj,
			Map data,
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
			final PrivilegedAction<Object> delegatedAction = new PrivilegedAction<Object>() {
				@Override
				public Object run() {
					final Setter setter = ReflectionTools.getSetter(
							obj.getClass(),
							propertyData,
							versionsReader.getSessionImplementor().getSessionFactory().getServiceRegistry()
					);

					// We only set a null value if the field is not primitive. Otherwise, we leave it intact.
					if ( value != null || !isPrimitive( setter, propertyData, obj.getClass() ) ) {
						setter.set( obj, value, null );
					}

					return null;
				}
			};

			if ( System.getSecurityManager() != null ) {
				AccessController.doPrivileged( delegatedAction );
			}
			else {
				delegatedAction.run();
			}
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
			Object id) {
		return null;
	}

	@Override
	public boolean hasPropertiesWithModifiedFlag() {
		return propertyData != null && propertyData.isUsingModifiedFlag();
	}

	@SuppressWarnings("unchecked")
	private boolean areEqual(Object newObj, Object oldObj) {
		// Should a JavaTypeDescriptor be specified in the property mapper, delegate there to make
		// certain that proper equality comparisons occur based on the descriptor semantics rather
		// than the generalized EqualsHelper#areEqual.
		if ( propertyData.getJavaTypeDescriptor() != null ) {
			return propertyData.getJavaTypeDescriptor().areEqual( newObj, oldObj );
		}

		// todo (6.0) - Confirm if this is still necessary as everything should use a JavaTypeDescriptor.
		//		This was maintained for legacy 5.2 behavior only.
		return Objects.equals( newObj, oldObj );
	}
}
