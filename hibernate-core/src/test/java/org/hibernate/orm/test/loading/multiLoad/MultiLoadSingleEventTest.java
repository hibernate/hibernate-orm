/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.loading.multiLoad;

import java.util.List;

import org.hibernate.annotations.NaturalId;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiLoadSingleEventTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { IdEvent.class, NaturalIdEvent.class };
	}

	@Test
	public void byId() {
		inTransaction( session -> session.persist( new IdEvent( 1 ) ) );

		inTransaction( session -> {
			List<IdEvent> events = session.byMultipleIds( IdEvent.class )
					.multiLoad( 1 );

			assertThat( events ).hasSize( 1 );
			assertThat( events.get( 0 ) ).isNotNull();
			assertThat( events ).extracting( "text" ).containsExactly( "text1" );
		} );
	}

	@Test
	public void byNaturalId() {
		inTransaction( session -> session.persist( new NaturalIdEvent( 1 ) ) );

		inTransaction( session -> {
			List<NaturalIdEvent> events = session.byMultipleNaturalId( NaturalIdEvent.class )
					.multiLoad( "code1" );

			assertThat( events ).hasSize( 1 );
			assertThat( events.get( 0 ) ).isNotNull();
			assertThat( events ).extracting( "id" ).containsExactly( 1 );
		} );
	}

	@Entity(name = "IdEvent")
	public static class IdEvent {

		@Id
		private Integer id;

		private String text;

		public IdEvent() {
		}

		public IdEvent(Integer id) {
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

	@Entity(name = "NaturalIdEvent")
	public static class NaturalIdEvent {

		@Id
		private Integer id;

		@NaturalId
		private String code;

		public NaturalIdEvent() {
		}

		public NaturalIdEvent(Integer id) {
			this.id = id;
			this.code = "code" + id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}
	}
}
