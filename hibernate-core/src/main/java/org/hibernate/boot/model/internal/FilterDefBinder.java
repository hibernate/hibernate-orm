/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.usertype.UserType;

import jakarta.persistence.AttributeConverter;

import static org.hibernate.boot.model.internal.AnnotationHelper.resolveAttributeConverter;
import static org.hibernate.boot.model.internal.AnnotationHelper.resolveBasicType;
import static org.hibernate.boot.model.internal.AnnotationHelper.resolveJavaType;
import static org.hibernate.boot.model.internal.AnnotationHelper.resolveUserType;

/**
 * @author Gavin King
 */
public class FilterDefBinder {

	@SuppressWarnings("unchecked")
	public static JdbcMapping resolveFilterParamType(Class<?> type, MetadataBuildingContext context) {
		if ( UserType.class.isAssignableFrom( type ) ) {
			return resolveUserType( (Class<UserType<?>>) type, context );
		}
		else if ( AttributeConverter.class.isAssignableFrom( type ) ) {
			return resolveAttributeConverter( (Class<? extends AttributeConverter<?,?>>) type, context );
		}
		else if ( JavaType.class.isAssignableFrom( type ) ) {
			return resolveJavaType( (Class<JavaType<?>>) type, context );
		}
		else {
			return resolveBasicType( type, context );
		}
	}
}
