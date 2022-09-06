/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import org.hibernate.MappingException;
import org.hibernate.PropertyValueException;
import org.hibernate.Session;
import org.hibernate.annotations.TenantId;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Value generation implementation for {@link TenantId}.
 *
 * @author Gavin King
 */
public class TenantIdGeneration implements AnnotationValueGeneration<TenantId>, ValueGenerator<Object> {

	private String entityName;
	private String propertyName;
	private Class<?> propertyType;

	@Override
	public void initialize(TenantId annotation, Class<?> propertyType, String entityName, String propertyName) {
		this.entityName = entityName;
		this.propertyName = propertyName;
		this.propertyType = propertyType;
	}

	@Override
	public void initialize(TenantId annotation, Class<?> propertyType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		return GenerationTiming.INSERT;
	}

	@Override
	public ValueGenerator<?> getValueGenerator() {
		return this;
	}

	@Override
	public Object generateValue(Session session, Object owner, Object currentValue) {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) session.getSessionFactory();
		JavaType<Object> descriptor = sessionFactory.getTypeConfiguration().getJavaTypeRegistry()
				.findDescriptor(propertyType);
		if ( descriptor==null ) {
			throw new MappingException( "unsupported tenant id property type: " + propertyType.getName() );
		}

		String tenantId = session.getTenantIdentifier(); //unfortunately this is always a string in old APIs
		if ( currentValue != null ) {
			CurrentTenantIdentifierResolver resolver = sessionFactory.getCurrentTenantIdentifierResolver();
			if ( resolver!=null && resolver.isRoot(tenantId) ) {
				// the "root" tenant is allowed to set the tenant id explicitly
				return currentValue;
			}
			String currentTenantId = descriptor.toString(currentValue);
			if ( !currentTenantId.equals(tenantId) ) {
				throw new PropertyValueException(
						"assigned tenant id differs from current tenant id: "
								+ currentTenantId + "!=" + tenantId,
						entityName, propertyName
				);
			}
		}
		return tenantId == null ? null : descriptor.fromString(tenantId); //convert to the model type
	}

	@Override
	public Object generateValue(Session session, Object owner) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean referenceColumnInSql() {
		return false;
	}

	@Override
	public String getDatabaseGeneratedReferencedColumnValue() {
		return null;
	}
}
