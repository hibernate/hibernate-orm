/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ops;

import java.util.List;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				EnumsParameterTest.Event.class,
				EnumsParameterTest.Organizer.class,
				EnumsParameterTest.Customer.class,
		}
)
@SessionFactory
@JiraKey(value = "HHH-15498")
public class EnumsParameterTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Organizer organizer = new Organizer( 1L, "Test Organizer" );
					Event event = new Event( 1L, "Test Event", organizer, Type.INDOOR );
					Event nullEvent = new Event( 2L, "Null Event", null, Type.OUTDOOR );
					session.persist( organizer );
					session.persist( event );
					session.persist( nullEvent );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testDeleteByEventType(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "DELETE FROM Event WHERE (:type IS NULL OR type = :type)" )
							.setParameter( "type", Type.INDOOR )
							.executeUpdate();
				}
		);

		scope.inTransaction(
				session -> {
					List<Event> events = session.createQuery( "select e FROM Event e", Event.class )
							.list();
					assertThat( events.size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	public void testDelete(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery(
									"DELETE FROM Event WHERE (cast(:type as String) IS NULL OR type = :type)" )
							.setParameter( "type", null )
							.executeUpdate();
				}
		);

		scope.inTransaction(
				session -> {
					List<Event> events = session.createQuery( "select e FROM Event e", Event.class )
							.list();
					assertThat( events.size() ).isEqualTo( 0 );
				}
		);
	}

	@Test
	public void testSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query query = session.createQuery(
							"SELECT c FROM Customer c WHERE (:phoneType IS NULL OR c.type = :phoneType)",
							Customer.class
					);
					query.setParameter( "phoneType", Customer.PhoneType.LAND_LINE );
					List<Customer> customerList = query.getResultList();
				}
		);
	}

	@Entity(name = "Organizer")
	@Table(name = "ORGANIZER_TABLE")
	public static class Organizer {
		@Id
		private Long id;

		private String name;

		public Organizer() {
		}

		public Organizer(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "Event")
	@Table(name = "EVENT_CLASS")
	public static class Event {
		@Id
		private Long id;

		private String name;

		@Column(name = "TYPE_COLUMN", nullable = false)
		@Enumerated(EnumType.STRING)
		private Type type;

		@ManyToOne(fetch = FetchType.LAZY)
		@OnDelete(action = OnDeleteAction.CASCADE)
		@JoinColumn(name = "OrganizerId", referencedColumnName = "Id")
		private Organizer organizer;

		public Event() {
		}

		public Event(Long id, String name, Organizer organizer, Type type) {
			this.id = id;
			this.name = name;
			this.organizer = organizer;
			this.type = type;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Organizer getOrganizer() {
			return organizer;
		}

		public void setOrganizer(Organizer organizer) {
			this.organizer = organizer;
		}

		public Type getType() {
			return type;
		}

		public Event setType(Type type) {
			this.type = type;
			return this;
		}
	}

	enum Type {
		INDOOR, OUTDOOR
	}

	@Entity(name = "Customer")
	@Table(name = "CUSTOMER")
	public static class Customer {

		@Id
		@Column(name = "id")
		int id;

		@Enumerated(EnumType.STRING)
		@Column(name = "type")
		private PhoneType type;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public PhoneType getType() {
			return type;
		}

		public void setType(PhoneType type) {
			this.type = type;
		}

		public enum PhoneType {
			MOBILE,
			LAND_LINE;
		}
	}

}
