/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.util.Comparator;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.AbstractIdentifiableJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.IdentifiableJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.MappedSuperclassJavaDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Chris Cranford
 */
public class MappedSuperclassJavaDescriptorImpl<J>
		extends AbstractIdentifiableJavaDescriptor<J>
		implements MappedSuperclassJavaDescriptor<J> {

	public MappedSuperclassJavaDescriptorImpl(
			String typeName,
			Class<? super J> javaType,
			IdentifiableJavaDescriptor<? super J> superTypeDescriptor,
			MutabilityPlan mutabilityPlan,
			Comparator comparator) {
		super( typeName, javaType, superTypeDescriptor, mutabilityPlan, comparator );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		throw new UnsupportedOperationException( "SqlTypeDescriptor must be specified for ManagedSuperclassType" );
	}

	@Override
	public <X> X unwrap(J value, Class<X> type, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <X> J wrap(X value, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public String extractLoggableRepresentation(J value) {
		return "MappedSuperclass(" + value + ")";
	}
}
