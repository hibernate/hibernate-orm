/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.internal;

import java.util.Comparator;

import org.hibernate.sqm.NotYetImplementedException;
import org.hibernate.type.descriptor.java.spi.AbstractManagedJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.MutabilityPlan;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.spi.WrapperOptions;
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
			EmbeddableJavaDescriptor<? super J> superTypeDescriptor,
			MutabilityPlan mutabilityPlan,
			Comparator comparator) {
		super( typeName, javaType, superTypeDescriptor, mutabilityPlan, comparator );
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
	public J wrap(Object value, WrapperOptions options) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public <X> X unwrap(J value, Class<X> type, WrapperOptions options) {
		throw new NotYetImplementedException(  );
	}
}
