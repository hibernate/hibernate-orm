/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.internal;

import java.util.Map;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.sqm.NotYetImplementedException;
import org.hibernate.type.descriptor.java.spi.AbstractManagedJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class EmbeddableJavaDescriptorImpl<J>
		extends AbstractManagedJavaDescriptor<J>
		implements EmbeddableJavaDescriptor<J> {
	public EmbeddableJavaDescriptorImpl(
			String typeName,
			Class javaType,
			EmbeddableJavaDescriptor<? super J> superTypeDescriptor) {
		super( typeName, javaType, superTypeDescriptor, null, null );
	}

	@Override
	@SuppressWarnings("unchecked")
	public EmbeddableJavaDescriptor<? super J> getSuperType() {
		return (EmbeddableJavaDescriptor<? super J>) super.getSuperType();
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		// none
		return null;
	}

	@Override
	public String extractLoggableRepresentation(Object value) {
		return "{embeddable(" + value + ")}";
	}

	@Override
	public String toString(Object value) {
		return null;
	}

	@Override
	public J fromString(String string) {
		return null;
	}

	@Override
	public J wrap(Object value, SharedSessionContractImplementor session) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public <X> X unwrap(J value, Class<X> type, SharedSessionContractImplementor session) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public boolean isInstance(Object value) {
		// Dynamic Components do not have a java-type but is represented by a HashMap
		// We check this use case here since its applicable to Embeddable only.
		if ( getJavaType() == null && ( value instanceof Map ) ) {
			return true;
		}

		return super.isInstance( value );
	}

	@Override
	public boolean isAssignableFrom(Class checkType) {
		// Dynamic Components do not have a java-type but is represented by a HashMap
		// We check this use case here since its applicable to Embeddable only.
		if ( getJavaType() == null && Map.class.isAssignableFrom( checkType ) ) {
			return true;
		}

		return super.isAssignableFrom( checkType );
	}
}
