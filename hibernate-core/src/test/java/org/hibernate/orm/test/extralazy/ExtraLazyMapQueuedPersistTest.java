/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.extralazy;

import java.time.LocalDate;
import java.util.TreeMap;
import java.util.SortedMap;


import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.SortNatural;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;


/**
 * @author Guillaume Toison
 */
@Jira( "https://hibernate.atlassian.net/browse/HHH-18885" )
@DomainModel( annotatedClasses = {
		ExtraLazyMapQueuedPersistTest.Person.class,
		ExtraLazyMapQueuedPersistTest.Event.class,
} )
@SessionFactory( useCollectingStatementInspector = true )
public class ExtraLazyMapQueuedPersistTest {
	@Test
	public void testQueuedPersistOperation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Person person = new Person( 1L );

			session.persist ( person );
		} );

		scope.inTransaction( session -> {
			final Person person = session.find( Person.class, 1L );

			LocalDate date = LocalDate.of( 2024, 1, 1 );
			Event event = new Event( 1L, person, date );

			person.getEvents().put( date, event );
		} );
	}

	@Entity( name = "Person" )
	public static class Person {
		@Id
		private Long id;

		@OneToMany(mappedBy = "person")
		@MapKeyColumn(name = "date_col")
		@SortNatural
		@LazyCollection(LazyCollectionOption.EXTRA)
		private SortedMap<LocalDate, Event> events = new TreeMap<>();

		public Person() {
		}

		public Person(Long id) {
			this.id = id;
		}

		public SortedMap<LocalDate, Event> getEvents() {
			return events;
		}

		public void setEvents(SortedMap<LocalDate, Event> events) {
			this.events = events;
		}
	}

	@Entity( name = "Event" )
	public static class Event {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "person_id")
		private Person person;

		@Column( name="date_col" )
		private LocalDate date;

		public Event() {
		}

		public Event(Long id, Person person, LocalDate date) {
			this.id = id;
			this.person = person;
			this.date = date;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}

		public LocalDate getDate() {
			return date;
		}

		public void setDate(LocalDate date) {
			this.date = date;
		}
	}
}
