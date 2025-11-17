/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.manytomany;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.TypedQuery;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				ManyToManyUnidirectionalFetchTest.Event.class,
				ManyToManyUnidirectionalFetchTest.Speaker.class
		}
)
@SessionFactory
public class ManyToManyUnidirectionalFetchTest {

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Event event = new Event( 1L, "Hibernate" );
					session.persist( event );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testJoinFetchEmptyCollection(SessionFactoryScope scope) {
		scope.inTransaction(
				sesison -> {
					TypedQuery<Event> query = sesison.createQuery(
							"SELECT e FROM Event e LEFT JOIN FETCH e.speakers  WHERE e.id = :oid", Event.class );
					query.setParameter( "oid", 1L );
					Event event = query.getSingleResult();
					assertNotNull( event );
					assertEquals( 0, event.getSpeakers().size() );
				}
		);
	}

	@Test
	public void testJoinEmptyCollection(SessionFactoryScope scope) {
		scope.inTransaction(
				sesison -> {
					TypedQuery<Event> query = sesison.createQuery(
							"SELECT e FROM Event e LEFT JOIN e.speakers  WHERE e.id = :oid", Event.class );
					query.setParameter( "oid", 1L );
					Event event = query.getSingleResult();
					assertNotNull( event );
					Set<Speaker> speakers = event.getSpeakers();
					assertEquals( 0, speakers.size() );
				}
		);
	}

	@Test
	public void testJoinFetch(SessionFactoryScope scope) {
		addSpeakerToEvent( scope );
		scope.inTransaction(
				sesison -> {
					TypedQuery<Event> query = sesison.createQuery(
							"SELECT e FROM Event e LEFT JOIN FETCH e.speakers  WHERE e.id = :oid", Event.class );
					query.setParameter( "oid", 1L );
					Event event = query.getSingleResult();
					assertNotNull( event );
					Set<Speaker> speakers = event.getSpeakers();
					assertEquals( 1, speakers.size() );
					assertThat( speakers.iterator().next().getName(), is( "Steve" ) );
				}
		);
	}

	@Test
	public void testJoin(SessionFactoryScope scope) {
		addSpeakerToEvent( scope );
		scope.inTransaction(
				sesison -> {
					TypedQuery<Event> query = sesison.createQuery(
							"SELECT e FROM Event e LEFT JOIN e.speakers  WHERE e.id = :oid", Event.class );
					query.setParameter( "oid", 1L );
					Event event = query.getSingleResult();
					assertNotNull( event );
					Set<Speaker> speakers = event.getSpeakers();

					assertEquals( 1, speakers.size() );

					assertThat( speakers.iterator().next().getName(), is( "Steve" ) );
				}
		);
	}

	private void addSpeakerToEvent(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Event event = session.find( Event.class, 1l );
					Speaker speaker = new Speaker( 2l, "Steve" );
					event.addSpeaker( speaker );
					session.persist( speaker );
				}
		);
	}

	@Entity(name = "Event")
	public static class Event {

		@Id
		private Long id;

		private String name;

		public Event() {
		}

		public Event(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		@ManyToMany(targetEntity = Speaker.class, fetch = FetchType.LAZY, cascade = {
				CascadeType.PERSIST,
				CascadeType.REFRESH,
				CascadeType.REMOVE
		})
		private Set<Speaker> speakers = new HashSet<>();

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

		public Set<Speaker> getSpeakers() {
			return Collections.unmodifiableSet( speakers );
		}

		public void setSpeakers(Set<Speaker> speakers) {
			this.speakers = speakers;
		}

		public void addSpeaker(Speaker speaker) {
			this.speakers.add( speaker );
		}
	}

	@Entity(name = "Speaker")
	public static class Speaker {

		@Id
		private Long id;

		private String name;

		public Speaker() {
		}

		public Speaker(Long id, String name) {
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
}
