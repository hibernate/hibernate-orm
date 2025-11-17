/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.loading.multiLoad;

import java.util.List;

import org.hibernate.annotations.NaturalId;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = { MultiLoadSingleEventTest.IdEvent.class, MultiLoadSingleEventTest.NaturalIdEvent.class })
@SessionFactory
public class MultiLoadSingleEventTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void byId(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> session.persist( new IdEvent( 1 ) ) );

		factoryScope.inTransaction( (session) -> {
			var events = session.findMultiple( IdEvent.class,
					List.of(1)
			);

			assertThat( events ).hasSize( 1 );
			assertThat( events.get( 0 ) ).isNotNull();
			assertThat( events ).extracting( "text" ).containsExactly( "text1" );
		} );
	}

	@Test
	public void byNaturalId(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> session.persist( new NaturalIdEvent( 1 ) ) );

		factoryScope.inTransaction( session -> {
			var events = session.byMultipleNaturalId( NaturalIdEvent.class )
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
