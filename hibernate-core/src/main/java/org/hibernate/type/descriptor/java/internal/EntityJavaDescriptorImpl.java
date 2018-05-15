/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.internal;

import java.util.Comparator;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.spi.AbstractIdentifiableJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.IdentifiableJavaDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class EntityJavaDescriptorImpl<J>
		extends AbstractIdentifiableJavaDescriptor<J>
		implements EntityJavaDescriptor<J> {
	private final String entityName;

	public EntityJavaDescriptorImpl(
			String typeName,
			String entityName,
			Class<? super J> javaType,
			IdentifiableJavaDescriptor<? super J> superTypeDescriptor,
			MutabilityPlan mutabilityPlan,
			Comparator comparator) {
		super(
				typeName,
				javaType,
				superTypeDescriptor,
				mutabilityPlan,
				comparator
		);
		this.entityName = entityName;
	}

	@Override
	public String getEntityName() {
		return entityName;
	}

	@Override
	public String getJpaEntityName() {
		return entityName;
	}

	@Override
	public int extractHashCode(Object value) {
		return value.hashCode();
	}

	@Override
	public boolean areEqual(Object one, Object another) {
		return false;
	}

	@Override
	public String extractLoggableRepresentation(Object entity) {
		return "Entity(" + entity + ")";
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		throw new UnsupportedOperationException( "SqlTypeDescriptor must be specified for EntityType" );
	}

	@Override
	public String toString(Object value) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public J fromString(String string) {
		throw new UnsupportedOperationException( "Entity type cannot be read from String" );
	}

	@Override
	public Object wrap(Object value, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public Object unwrap(Object value, Class type, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
	}
}
