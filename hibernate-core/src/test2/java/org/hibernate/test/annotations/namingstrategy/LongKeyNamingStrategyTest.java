/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id$
package org.hibernate.test.annotations.namingstrategy;

import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.Environment;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * Test harness for HHH-11089.
 *
 * @author Vlad Mihalcea
 */
@TestForIssue( jiraKey = "HHH-11089" )
public class LongKeyNamingStrategyTest extends BaseUnitTestCase {

	private ServiceRegistry serviceRegistry;

	@Before
    public void setUp() {
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
	}

	@After
    public void tearDown() {
        if ( serviceRegistry != null ) {
			ServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}
    @Test
	public void testWithCustomNamingStrategy() throws Exception {
		Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass(Address.class)
				.addAnnotatedClass(Person.class)
				.getMetadataBuilder()
				.applyImplicitNamingStrategy( new LongIdentifierNamingStrategy() )
				.build();

		org.hibernate.mapping.ForeignKey foreignKey =
				(org.hibernate.mapping.ForeignKey) metadata.getEntityBinding( Address.class.getName()).getTable().getForeignKeyIterator().next();
		assertEquals( "FK_way_longer_than_the_30_char", foreignKey.getName() );

		UniqueKey uniqueKey = metadata.getEntityBinding( Address.class.getName()).getTable().getUniqueKeyIterator().next();
		assertEquals( "UK_way_longer_than_the_30_char", uniqueKey.getName() );

		org.hibernate.mapping.Index index = metadata.getEntityBinding( Address.class.getName()).getTable().getIndexIterator().next();
		assertEquals( "IDX_way_longer_than_the_30_cha", index.getName() );
	}

	@Entity(name = "Address")
	@Table(uniqueConstraints = @UniqueConstraint(
			name = "UK_way_longer_than_the_30_characters_limit",
			columnNames = {
					"city", "streetName", "streetNumber"
			}),
			indexes = @Index( name = "IDX_way_longer_than_the_30_characters_limit", columnList = "city, streetName, streetNumber")
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
