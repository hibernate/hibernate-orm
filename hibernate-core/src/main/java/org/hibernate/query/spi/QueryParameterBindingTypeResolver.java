/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import org.hibernate.Incubating;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.type.BindableType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A resolver for {@link BindableType} based on a parameter value being bound, when no explicit type information is
 * supplied.
 *
 * @apiNote This interface was originally a supertype of {@link org.hibernate.engine.spi.SessionFactoryImplementor},
 *          but this is now a deprecated relationship. Its functionality should now be accessed via its new subtype
 *          {@link org.hibernate.metamodel.spi.MappingMetamodelImplementor}.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface QueryParameterBindingTypeResolver {
	<T> BindableType<? super T> resolveParameterBindType(T bindValue);
	<T> BindableType<T> resolveParameterBindType(Class<T> clazz);

	@Deprecated(since = "7.0", forRemoval = true)
	TypeConfiguration getTypeConfiguration();
	@Deprecated(since = "7.0", forRemoval = true)
	MappingMetamodel getMappingMetamodel();
}
