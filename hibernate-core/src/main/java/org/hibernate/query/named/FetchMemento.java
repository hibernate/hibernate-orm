/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named;

import java.util.function.Consumer;

import jakarta.persistence.sql.MemberMapping;
import org.hibernate.SessionFactory;
import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.results.spi.FetchBuilder;

/**
 * @author Steve Ebersole
 */
public interface FetchMemento extends ModelPartReferenceMemento {
	/**
	 * The parent node for the fetch
	 */
	interface Parent extends ModelPartReferenceMemento {
		Class<?> getResultJavaType();
	}

	/**
	 * Resolve the fetch-memento into the result-graph-node builder
	 */
	FetchBuilder resolve(Parent parent, Consumer<String> querySpaceConsumer, ResultSetMappingResolutionContext context);

	default MemberMapping<?> toJpaMemberMapping(Parent parent, SessionFactory sessionFactory) {
		throw new UnsupportedOperationException( "Not implemented yet - " + getClass().getName() );
	}
}
