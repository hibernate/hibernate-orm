/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.immutable.xml;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.PersistenceException;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DomainModel(xmlMappings = "mappings/immutable/ImmutableElementCollectionXmlTest.orm.xml")
@SessionFactory
public class ImmutableElementCollectionXmlTest {

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testImmutableElementCollection(SessionFactoryScope scope) {
		var author = new Author();
		author.setName( "Andrea" );
		author.getAliases().add( "b8" );
		author.getAliases().add( "drea" );

		scope.inTransaction( session -> session.persist( author ) );

		PersistenceException ex = assertThrows( PersistenceException.class, () -> scope.inTransaction(
				session -> {
					Author a = session.find( Author.class, author.getId() );
					assertThat( a.getAliases() ).hasSize( 2 );
					a.getAliases().add( "newAlias" );
				}
		) );
		assertThat( ex.getMessage() ).contains( "Immutable collection was modified" );

		scope.inTransaction(
				session -> {
					Author a = session.find( Author.class, author.getId() );
					assertThat( a.getAliases() ).hasSize( 2 );
				}
		);
	}

	public static class Author {
		private int id;
		private String name;
		private List<String> aliases = new ArrayList<>();

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<String> getAliases() {
			return aliases;
		}

		public void setAliases(List<String> aliases) {
			this.aliases = aliases;
		}
	}
}
