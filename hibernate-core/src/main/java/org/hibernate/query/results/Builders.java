/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import javax.persistence.AttributeConverter;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class Builders {
	public static ScalarResultBuilder scalar(String columnAlias) {
		return new StandardScalarResultBuilder( columnAlias );
	}

	public static ScalarResultBuilder scalar(
			String columnAlias,
			BasicType<?> type) {
		return new StandardScalarResultBuilder( columnAlias, type );
	}

	public static ScalarResultBuilder scalar(
			String columnAlias,
			Class<?> javaType,
			SessionFactoryImplementor factory) {
		final JavaTypeDescriptor<?> javaTypeDescriptor = factory.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( javaType );

		return new StandardScalarResultBuilder( columnAlias, javaTypeDescriptor );
	}

	public static <C> ResultBuilder scalar(
			String columnAlias,
			Class<C> relationalJavaType,
			AttributeConverter<?, C> converter,
			SessionFactoryImplementor sessionFactory) {
		return ConvertedResultBuilder.from( columnAlias, relationalJavaType, converter, sessionFactory );
	}

	public static <C> ResultBuilder scalar(
			String columnAlias,
			Class<C> relationalJavaType,
			Class<? extends AttributeConverter<?, C>> converterJavaType,
			SessionFactoryImplementor sessionFactory) {
		return ConvertedResultBuilder.from( columnAlias, relationalJavaType, converterJavaType, sessionFactory );
	}

	public static ScalarResultBuilder scalar(int position) {
		// will be needed for interpreting legacy HBM <resultset/> mappings
		throw new NotYetImplementedFor6Exception();
	}

	public static ScalarResultBuilder scalar(int position, BasicType<?> type) {
		// will be needed for interpreting legacy HBM <resultset/> mappings
		throw new NotYetImplementedFor6Exception();
	}

	public static EntityResultBuilder entity(String tableAlias, String entityName) {
		throw new NotYetImplementedFor6Exception( );
	}

	public static LegacyFetchBuilder fetch(String tableAlias, String ownerTableAlias, String joinPropertyName) {
		throw new NotYetImplementedFor6Exception( );
	}
}
