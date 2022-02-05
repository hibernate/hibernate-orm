/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.MarkerObject;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.sql.InFragment;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Operations needed by persisters for working with discriminators.
 *
 * @author Gavin King
 */
class DiscriminatorHelper {

	static final Object NULL_DISCRIMINATOR = new MarkerObject( "<null discriminator>" );
	static final Object NOT_NULL_DISCRIMINATOR = new MarkerObject( "<not null discriminator>" );

	static BasicType<?> getDiscriminatorType(PersistentClass persistentClass) {
		Type discriminatorType = persistentClass.getDiscriminator().getType();
		if ( discriminatorType instanceof BasicType ) {
			return (BasicType<?>) discriminatorType;
		}
		else {
			throw new MappingException( "Illegal discriminator type: " + discriminatorType.getName() );
		}
	}

	static String getDiscriminatorSQLValue(
			PersistentClass persistentClass,
			Dialect dialect,
			SessionFactoryImplementor factory) {
		if ( persistentClass.isDiscriminatorValueNull() ) {
			return InFragment.NULL;
		}
		else if ( persistentClass.isDiscriminatorValueNotNull() ) {
			return InFragment.NOT_NULL;
		}
		else {
			return discriminatorSqlLiteral(
					getDiscriminatorType( persistentClass ),
					persistentClass,
					dialect,
					factory.getWrapperOptions()
			);
		}
	}

	private static Object parseDiscriminatorValue(PersistentClass persistentClass) {
		BasicType<?> discriminatorType = getDiscriminatorType( persistentClass );
		try {
			return discriminatorType.getJavaTypeDescriptor().fromString( persistentClass.getDiscriminatorValue() );
		}
		catch (Exception e) {
			throw new MappingException( "Could not parse discriminator value", e );
		}
	}

	static Object getDiscriminatorValue(PersistentClass persistentClass) {
		if ( persistentClass.isDiscriminatorValueNull() ) {
			return NULL_DISCRIMINATOR;
		}
		else if ( persistentClass.isDiscriminatorValueNotNull() ) {
			return NOT_NULL_DISCRIMINATOR;
		}
		else {
			return parseDiscriminatorValue( persistentClass );
		}
	}

	private static <T> String discriminatorSqlLiteral(
			BasicType<T> discriminatorType,
			PersistentClass persistentClass,
			Dialect dialect,
			WrapperOptions wrapperOptions) {
		JavaType<T> javaType = discriminatorType.getJavaTypeDescriptor();
		try {
			return discriminatorType.getJdbcType()
					.getJdbcLiteralFormatter( javaType )
					.toJdbcLiteral(
							javaType.fromString( persistentClass.getDiscriminatorValue() ),
							dialect,
							wrapperOptions
					);
		}
		catch (Exception e) {
			throw new MappingException( "Could not format discriminator value to SQL string", e );
		}
	}
}
