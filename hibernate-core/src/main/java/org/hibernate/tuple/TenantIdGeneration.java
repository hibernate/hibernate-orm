/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import org.hibernate.PropertyValueException;
import org.hibernate.Session;
import org.hibernate.annotations.TenantId;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Value generation implementation for {@link TenantId}.
 *
 * @author Gavin King
 */
public class TenantIdGeneration implements AnnotationValueGeneration<TenantId>, ValueGenerator<Object> {

	private String entityName;
	private String propertyName;

	@Override
	public void initialize(TenantId annotation, Class<?> propertyType, String entityName, String propertyName) {
		this.entityName = entityName;
		this.propertyName = propertyName;
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
		String identifier = session.getTenantIdentifier();
		if ( currentValue != null ) {
			CurrentTenantIdentifierResolver resolver =
					((SessionFactoryImplementor) session.getSessionFactory())
							.getCurrentTenantIdentifierResolver();
			if ( resolver!=null && resolver.isRoot( session.getTenantIdentifier() ) ) {
				// the "root" tenant is allowed to set the tenant id explicitly
				return currentValue;
			}
			if ( !currentValue.equals(identifier) ) {
				throw new PropertyValueException(
						"assigned tenant id differs from current tenant id: "
								+ currentValue + "!=" + identifier,
						entityName, propertyName
				);
			}
		}
		return identifier;
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
