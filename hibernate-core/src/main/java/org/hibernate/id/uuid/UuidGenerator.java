/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.id.uuid;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.UUID;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.spi.CustomIdGeneratorCreationContext;
import org.hibernate.id.factory.spi.StandardGenerator;
import org.hibernate.type.descriptor.java.UUIDJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.UUIDJavaTypeDescriptor.ValueTransformer;

import static org.hibernate.annotations.UuidGenerator.Style.TIME;

/**
 * UUID-based IdentifierGenerator
 *
 * @see org.hibernate.annotations.UuidGenerator
 */
public class UuidGenerator implements StandardGenerator {
	interface ValueGenerator {
		UUID generateUuid(SharedSessionContractImplementor session);
	}

	private final ValueGenerator generator;
	private final ValueTransformer valueTransformer;

	public UuidGenerator(
			org.hibernate.annotations.UuidGenerator config,
			Member idMember,
			CustomIdGeneratorCreationContext creationContext) {
		if ( config.style() == TIME ) {
			generator = new CustomVersionOneStrategy();
		}
		else {
			generator = StandardRandomStrategy.INSTANCE;
		}

		final Class<?> propertyType;
		if ( idMember instanceof Method ) {
			propertyType = ( (Method) idMember ).getReturnType();
		}
		else {
			propertyType = ( (Field) idMember ).getType();
		}

		if ( UUID.class.isAssignableFrom( propertyType ) ) {
			valueTransformer = UUIDJavaTypeDescriptor.PassThroughTransformer.INSTANCE;
		}
		else if ( String.class.isAssignableFrom( propertyType ) ) {
			valueTransformer = UUIDJavaTypeDescriptor.ToStringTransformer.INSTANCE;
		}
		else if ( byte[].class.isAssignableFrom( propertyType ) ) {
			valueTransformer = UUIDJavaTypeDescriptor.ToBytesTransformer.INSTANCE;
		}
		else {
			throw new HibernateException( "Unanticipated return type [" + propertyType.getName() + "] for UUID conversion" );
		}
	}

	public Object generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
		return valueTransformer.transform( generator.generateUuid( session ) );
	}
}
