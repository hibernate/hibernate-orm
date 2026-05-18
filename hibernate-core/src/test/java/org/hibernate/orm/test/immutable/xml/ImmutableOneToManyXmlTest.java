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

@DomainModel(xmlMappings = "mappings/immutable/ImmutableOneToManyXmlTest.orm.xml")
@SessionFactory
public class ImmutableOneToManyXmlTest {

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testImmutableOneToManyCollection(SessionFactoryScope scope) {
		var country = new Country();
		country.setName( "Italy" );

		var lombardia = new Region();
		lombardia.setName( "Lombardia" );
		var lazio = new Region();
		lazio.setName( "Lazio" );
		var toscana = new Region();
		toscana.setName( "Toscana" );

		country.getRegions().add( lombardia );
		country.getRegions().add( lazio );
		country.getRegions().add( toscana );

		scope.inTransaction( session -> session.persist( country ) );

		PersistenceException ex = assertThrows( PersistenceException.class, () -> scope.inTransaction(
				session -> {
					Country c = session.find( Country.class, country.getId() );
					assertThat( c.getRegions() ).hasSize( 3 );

					Region piemonte = new Region();
					piemonte.setName( "Piemonte" );
					session.persist( piemonte );
					c.getRegions().add( piemonte );
				}
		) );
		assertThat( ex.getMessage() ).contains( "Immutable collection was modified" );

		ex = assertThrows( PersistenceException.class, () -> scope.inTransaction(
				session -> {
					Country c = session.find( Country.class, country.getId() );
					assertThat( c.getRegions() ).hasSize( 3 );
					c.getRegions().remove( 0 );
				}
		) );
		assertThat( ex.getMessage() ).contains( "Immutable collection was modified" );

		scope.inTransaction(
				session -> {
					Country c = session.find( Country.class, country.getId() );
					assertThat( c.getRegions() ).hasSize( 3 );
				}
		);
	}

	public static class Country {
		private int id;
		private String name;
		private List<Region> regions = new ArrayList<>();

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

		public List<Region> getRegions() {
			return regions;
		}

		public void setRegions(List<Region> regions) {
			this.regions = regions;
		}
	}

	public static class Region {
		private int id;
		private String name;

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
	}
}
