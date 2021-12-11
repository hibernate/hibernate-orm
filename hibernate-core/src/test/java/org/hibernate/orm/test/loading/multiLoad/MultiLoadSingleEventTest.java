/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.loading.multiLoad;

import java.util.List;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiLoadSingleEventTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Event.class };
	}

	@Test
	public void test() {
		inTransaction( session -> session.persist( new Event( 1 ) ) );

		inTransaction( session -> {
			List<Event> events = session.byMultipleIds( Event.class )
					.multiLoad( 1 );

			assertThat( events ).hasSize( 1 );
			assertThat( events.get( 0 ) ).isNotNull();
			assertThat( events ).extracting( "text" ).containsExactly( "text1" );
		} );
	}

	@Entity(name = "Event")
	public static class Event {

		@Id
		private Integer id;

		private String text;

		public Event() {
		}

		public Event(Integer id) {
			this.id = id;
			this.text = "text" + id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}
}
