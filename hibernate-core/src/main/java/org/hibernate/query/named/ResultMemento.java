/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named;

import java.util.function.Consumer;

import jakarta.persistence.sql.MappingElement;
import jakarta.persistence.sql.ResultSetMapping;
import org.hibernate.Incubating;
import org.hibernate.SessionFactory;
import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.results.spi.ResultBuilder;

/**
 * Models a SQL ResultSet mapping generally defined via {@linkplain jakarta.persistence.SqlResultSetMapping annotations}.
 * May also be created via
 * .
 *
 * [1] Or through
 *
 * @since 6.0
 *
 * @author Steve Ebersole
 */
@Incubating
public interface ResultMemento extends ResultMappingMementoNode {

	Class<?> getResultJavaType();

	ResultBuilder resolve(Consumer<String> querySpaceConsumer, ResultSetMappingResolutionContext context);

	default <R> boolean canBeTreatedAsResultSetMapping(Class<R> resultType, SessionFactory sessionFactory) {
		return resultType.isAssignableFrom( getResultJavaType() );
	}

	<R> ResultSetMapping<R> toJpaMapping(SessionFactory sessionFactory);

	default <R> MappingElement<R> toJpaMappingElement(SessionFactory sessionFactory) {
		throw new UnsupportedOperationException();
	}
}
