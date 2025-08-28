/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.criteria.JoinType;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 * @author Chris Cranford
 */
@Jpa(annotatedClasses = {
		QueryAuditTest.Customer.class,
		QueryAuditTest.Address.class
}, integrationSettings = {
		@Setting(name = EnversSettings.AUDIT_STRATEGY, value = "org.hibernate.envers.strategy.ValidityAuditStrategy")
})
public class QueryAuditTest {
	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Address address = new Address();
			address.setId(1L);
			address.setCountry("România");
			address.setCity("Cluj-Napoca");
			address.setStreet("Bulevardul Eroilor");
			address.setStreetNumber("1 A");
			entityManager.persist(address);

			Customer customer = new Customer();
			customer.setId(1L);
			customer.setFirstName("John");
			customer.setLastName("Doe");
			customer.setAddress(address);

			entityManager.persist(customer);
		});

		scope.inTransaction( entityManager -> {
			Customer customer = entityManager.find(Customer.class, 1L);
			customer.setLastName("Doe Jr.");
		});

		scope.inTransaction( entityManager -> {
			Customer customer = entityManager.getReference(Customer.class, 1L);
			entityManager.remove(customer);
		});

		List<Number> revisions = scope.fromTransaction( entityManager -> {
			return AuditReaderFactory.get(entityManager).getRevisions(
				Customer.class,
				1L
			);
		});

		scope.inTransaction( entityManager -> {
			//tag::entities-at-revision-example[]
			Customer customer = (Customer) AuditReaderFactory
			.get(entityManager)
			.createQuery()
			.forEntitiesAtRevision(Customer.class, revisions.get(0))
			.getSingleResult();

			assertEquals("Doe", customer.getLastName());
			//end::entities-at-revision-example[]
		});

		scope.inTransaction( entityManager -> {
			//tag::entities-filtering-example[]
			List<Customer> customers = AuditReaderFactory
			.get(entityManager)
			.createQuery()
			.forRevisionsOfEntity(Customer.class, true, true)
			.add(AuditEntity.property("firstName").eq("John"))
			.getResultList();

			assertEquals(2, customers.size());
			assertEquals("Doe", customers.get(0).getLastName());
			assertEquals("Doe Jr.", customers.get(1).getLastName());
			//end::entities-filtering-example[]
		});

		scope.inTransaction( entityManager -> {
			//tag::entities-filtering-by-entity-example[]
			Address address = entityManager.getReference(Address.class, 1L);

			List<Customer> customers = AuditReaderFactory
			.get(entityManager)
			.createQuery()
			.forRevisionsOfEntity(Customer.class, true, true)
			.add(AuditEntity.property("address").eq(address))
			.getResultList();

			assertEquals(2, customers.size());
			//end::entities-filtering-by-entity-example[]
		});

		scope.inTransaction( entityManager -> {
			//tag::entities-filtering-by-entity-identifier-example[]
			List<Customer> customers = AuditReaderFactory
			.get(entityManager)
			.createQuery()
			.forRevisionsOfEntity(Customer.class, true, true)
			.add(AuditEntity.relatedId("address").eq(1L))
			.getResultList();

			assertEquals(2, customers.size());
			//end::entities-filtering-by-entity-identifier-example[]
		});

		scope.inTransaction( entityManager -> {
			//tag::entities-in-clause-filtering-by-entity-identifier-example[]
			List<Customer> customers = AuditReaderFactory
			.get(entityManager)
			.createQuery()
			.forRevisionsOfEntity(Customer.class, true, true)
			.add(AuditEntity.relatedId("address").in(new Object[] { 1L, 2L }))
			.getResultList();

			assertEquals(2, customers.size());
			//end::entities-in-clause-filtering-by-entity-identifier-example[]
		});

		scope.inTransaction( entityManager -> {
			//tag::entities-filtering-and-pagination[]
			List<Customer> customers = AuditReaderFactory
			.get(entityManager)
			.createQuery()
			.forRevisionsOfEntity(Customer.class, true, true)
			.addOrder(AuditEntity.property("lastName").desc())
			.add(AuditEntity.relatedId("address").eq(1L))
			.setFirstResult(1)
			.setMaxResults(2)
			.getResultList();

			assertEquals(1, customers.size());
			//end::entities-filtering-and-pagination[]
		});

		scope.inTransaction( entityManager -> {
			//tag::revisions-of-entity-query-example[]
			AuditQuery query = AuditReaderFactory.get(entityManager)
				.createQuery()
				.forRevisionsOfEntity(Customer.class, false, true);
			//end::revisions-of-entity-query-example[]
		});

		scope.inTransaction( entityManager -> {
			//tag::revisions-of-entity-query-by-revision-number-example[]
			Number revision = (Number) AuditReaderFactory
			.get(entityManager)
			.createQuery()
			.forRevisionsOfEntity(Customer.class, false, true)
			.addProjection(AuditEntity.revisionNumber().min())
			.add(AuditEntity.id().eq(1L))
			.add(AuditEntity.revisionNumber().gt(2))
			.getSingleResult();
			//end::revisions-of-entity-query-by-revision-number-example[]

			assertEquals(3, revision);
		});

		scope.inTransaction( entityManager -> {
			//tag::revisions-of-entity-query-minimize-example[]
			Number revision = (Number) AuditReaderFactory
			.get(entityManager)
			.createQuery()
			.forRevisionsOfEntity(Customer.class, false, true)
			.addProjection(AuditEntity.revisionNumber().min())
			.add(AuditEntity.id().eq(1L))
			.add(
				AuditEntity.property("createdOn")
				.minimize()
				.add(AuditEntity.property("createdOn")
					.ge(
						Timestamp.from(
							LocalDateTime.now()
								.minusDays(1)
								.toInstant(ZoneOffset.UTC)
							)
					)
				)
			)
			.getSingleResult();
			//end::revisions-of-entity-query-minimize-example[]

			assertEquals(1, revision);
		});

		scope.inTransaction( entityManager -> {
			{
				//tag::envers-querying-entity-relation-inner-join[]
				AuditQuery innerJoinAuditQuery = AuditReaderFactory
				.get(entityManager)
				.createQuery()
				.forEntitiesAtRevision(Customer.class, 1)
				.traverseRelation("address", JoinType.INNER);
				//end::envers-querying-entity-relation-inner-join[]
			}
			{
				//tag::envers-querying-entity-relation-left-join[]
				AuditQuery innerJoinAuditQuery = AuditReaderFactory
				.get(entityManager)
				.createQuery()
				.forEntitiesAtRevision(Customer.class, 1)
				.traverseRelation("address", JoinType.LEFT);
				//end::envers-querying-entity-relation-left-join[]
			}
		});

		scope.inTransaction( entityManager -> {
			//tag::envers-querying-entity-relation-join-restriction[]
			List<Customer> customers = AuditReaderFactory
			.get(entityManager)
			.createQuery()
			.forEntitiesAtRevision(Customer.class, 1)
			.traverseRelation("address", JoinType.INNER)
			.add(AuditEntity.property("country").eq("România"))
			.getResultList();
			//end::envers-querying-entity-relation-join-restriction[]

			assertEquals(1, customers.size());
		});

		scope.inTransaction( entityManager -> {
			//tag::aggregate-max-revision-with-entity-example[]
			List<Customer> results = AuditReaderFactory
				.get(entityManager)
				.createQuery()
				.forRevisionsOfEntity(Customer.class, true, false)
				.add(AuditEntity.revisionNumber().maximize().computeAggregationInInstanceContext())
				.getResultList();
			//end::aggregate-max-revision-with-entity-example[]
		});
	}

	@Audited
	@Entity(name = "Customer")
	public static class Customer {

		@Id
		private Long id;

		private String firstName;

		private String lastName;

		@Temporal(TemporalType.TIMESTAMP)
		@Column(name = "created_on")
		@CreationTimestamp
		private Date createdOn;

		@ManyToOne(fetch = FetchType.LAZY)
		private Address address;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public Date getCreatedOn() {
			return createdOn;
		}

		public void setCreatedOn(Date createdOn) {
			this.createdOn = createdOn;
		}

		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}
	}

	@Audited
	@Entity(name = "Address")
	public static class Address {

		@Id
		private Long id;

		private String country;

		private String city;

		private String street;

		private String streetNumber;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getCountry() {
			return country;
		}

		public void setCountry(String country) {
			this.country = country;
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}

		public String getStreet() {
			return street;
		}

		public void setStreet(String street) {
			this.street = street;
		}

		public String getStreetNumber() {
			return streetNumber;
		}

		public void setStreetNumber(String streetNumber) {
			this.streetNumber = streetNumber;
		}
	}
}
