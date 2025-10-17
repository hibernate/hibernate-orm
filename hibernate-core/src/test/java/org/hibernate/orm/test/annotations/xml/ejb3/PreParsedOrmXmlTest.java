/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.xml.ejb3;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.JiraKeyGroup;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKeyGroup(value = {
		@JiraKey(value = "HHH-14530"),
		@JiraKey(value = "HHH-14529")
})
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/annotations/xml/ejb3/pre-parsed-orm.xml"
)
@SessionFactory
public class PreParsedOrmXmlTest {

	@Test
	public void testPreParsedOrmXml(SessionFactoryScope scope) {
		// Just check that the entity can be persisted, which means the mapping file was taken into account
		NonAnnotatedEntity persistedEntity = new NonAnnotatedEntity( "someName" );

		scope.inTransaction( s -> s.persist( persistedEntity ) );

		scope.inTransaction( s -> {
			NonAnnotatedEntity retrievedEntity = s.find( NonAnnotatedEntity.class, persistedEntity.getId() );
			assertThat( retrievedEntity ).extracting( NonAnnotatedEntity::getName )
					.isEqualTo( persistedEntity.getName() );
		} );
	}

	public static class NonAnnotatedEntity {
		private long id;

		private String name;

		public NonAnnotatedEntity() {
		}

		public NonAnnotatedEntity(String name) {
			this.name = name;
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
