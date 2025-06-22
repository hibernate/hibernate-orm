/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sqm.param;

import java.util.Collections;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

/**
 * @author Nathan Xu
 */
@DomainModel(annotatedClasses = {
		EmptyEntityCollectionParameterTests.DbEntity.class,
		EmptyEntityCollectionParameterTests.ContentEntry.class
})
@SessionFactory
@JiraKey(value = "HHH-15232")
class EmptyEntityCollectionParameterTests {

	@Test
	void testNoPersistenceExceptionThrown(SessionFactoryScope scope) {
		// without fixing, the following exception would be thrown:
		// Converting `org.hibernate.type.descriptor.java.spi.JdbcTypeRecommendationException` to JPA `PersistenceException` :
		// Could not determine recommended JdbcType for `org.hibernate.orm.test.query.sqm.param.EmptyEntityCollectionParameterTests$ContentEntry`
		scope.inTransaction( session ->
									session.createQuery( "FROM DbEntity WHERE content IN (:vals)", DbEntity.class )
											.setParameter( "vals", Collections.emptyList() )
											.list()
		);
	}

	@Entity(name = "DbEntity")
	static class DbEntity {
		@Id
		long id;

		@OneToOne
		ContentEntry content;
	}

	@Entity(name = "ContentEntry")
	static class ContentEntry {
		@Id
		long id;

		String content;
	}
}
