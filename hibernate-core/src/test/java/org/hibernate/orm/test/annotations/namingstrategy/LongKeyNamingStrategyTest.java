/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.namingstrategy;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test harness for HHH-11089.
 *
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-11089")
@BaseUnitTest
public class LongKeyNamingStrategyTest {

	private ServiceRegistry serviceRegistry;

	@BeforeAll
	public void setUp() {
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
	}

	@AfterAll
	public void tearDown() {
		if ( serviceRegistry != null ) {
			ServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	public void testWithCustomNamingStrategy() {
		Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( Address.class )
				.addAnnotatedClass( Person.class )
				.getMetadataBuilder()
				.applyImplicitNamingStrategy( new LongIdentifierNamingStrategy() )
				.build();

		var foreignKey = metadata.getEntityBinding( Address.class.getName() ).getTable().getForeignKeyCollection()
				.iterator().next();
		assertThat( foreignKey.getName() ).isEqualTo( "FK_way_longer_than_the_30_char" );

		var uniqueKeys = metadata.getEntityBinding( Address.class.getName() ).getTable().getUniqueKeys().values();
		org.hibernate.mapping.Index index = null;
		if ( !uniqueKeys.isEmpty() ) {
			var uniqueKey = uniqueKeys.iterator().next();
			assertThat( uniqueKey.getName() ).isEqualTo( "UK_way_longer_than_the_30_char" );
			index = metadata.getEntityBinding( Address.class.getName() ).getTable().getIndexes().values().iterator()
					.next();
		}
		else {
			var indexes = metadata.getEntityBinding( Address.class.getName() ).getTable().getIndexes().values();
			assertThat( indexes.size() ).isEqualTo( 2 );

			org.hibernate.mapping.Index uniqueIndex = null;
			for ( var idx: indexes ) {
				if ( idx.isUnique() ) {
					uniqueIndex = idx;
				}
				else {
					index = idx;
				}
			}
			assertThat( uniqueIndex ).isNotNull();
			assertThat( uniqueIndex.getName() ).isEqualTo( "UK_way_longer_than_the_30_char" );
		}
		assertThat( index ).isNotNull();
		assertThat( index.getName() ).isEqualTo( "IDX_way_longer_than_the_30_cha" );
	}

	@Entity(name = "Address")
	@Table(uniqueConstraints = @UniqueConstraint(
			name = "UK_way_longer_than_the_30_characters_limit",
			columnNames = {
					"city", "streetName", "streetNumber"
			}),
			indexes = @Index(name = "IDX_way_longer_than_the_30_characters_limit",
					columnList = "city, streetName, streetNumber")
	)
	public class Address {

		@Id
		private Long id;

		private String city;

		private String streetName;

		private String streetNumber;

		@ManyToOne
		@JoinColumn(name = "person_id", foreignKey = @ForeignKey(name = "FK_way_longer_than_the_30_characters_limit"))
		private Person person;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}

		public String getStreetName() {
			return streetName;
		}

		public void setStreetName(String streetName) {
			this.streetName = streetName;
		}

		public String getStreetNumber() {
			return streetNumber;
		}

		public void setStreetNumber(String streetNumber) {
			this.streetNumber = streetNumber;
		}
	}

	@Entity
	public class Person {

		@Id
		private long id;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}
	}
}
