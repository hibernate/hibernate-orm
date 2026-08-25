/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.loaders;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that an entity custom loader defined in {@code mapping.xml} via
 * {@code <hql-select/>} is applied, mirroring the
 * {@link org.hibernate.annotations.HQLSelect} annotation.
 */
@DomainModel(xmlMappings = "org/hibernate/orm/test/loaders/entity-custom-select.xml")
@SessionFactory
@JiraKey( "HHH-20813" )
public class EntityCustomSelectXmlTest {

	@Test
	public void testCustomEntityLoader(SessionFactoryScope scope) {
		final Document document = new Document( 1L, "Hibernate" );
		scope.inTransaction( session -> session.persist( document ) );

		// the custom loader should find the (non-deleted) entity
		scope.inTransaction( session -> assertThat( session.find( Document.class, 1L ) ).isNotNull() );

		// soft-delete the entity - the custom loader filters on deleted = false
		scope.inTransaction( session -> session.find( Document.class, 1L ).deleted = true );

		// the custom loader should now filter out the soft-deleted entity,
		// which would still be returned by the default entity loader
		scope.inTransaction( session -> assertThat( session.find( Document.class, 1L ) ).isNull() );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	public static class Document {
		private Long id;
		private String name;
		private boolean deleted;

		public Document() {
		}

		public Document(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
