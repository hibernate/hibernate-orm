/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetchmode.toone;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@JiraKey("HHH-17064")
@DomainModel(
		annotatedClasses = {
				ManyToOneWithCircularityTest.Main.class,
				ManyToOneWithCircularityTest.Connector.class,
				ManyToOneWithCircularityTest.Sub.class,
		}
)
@SessionFactory
public class ManyToOneWithCircularityTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Connector connector = new Connector( 1L, "connector" );
					Sub sub = new Sub( 2L, "sub", connector );
					Main main = new Main( 3L, "main", connector, sub );

					session.persist( sub );
					session.persist( main );
					session.persist( connector );
				}
		);
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Main> result = session.createQuery( "select m from Main m", Main.class ).getResultList();

					assertThat( result.size() ).isEqualTo( 1 );
					Main main = result.get( 0 );
					Connector connector = main.getConnector();
					assertThat( Hibernate.isInitialized( connector ) ).isTrue();
					assertThat( connector ).isNotNull();
					Sub sub = main.getSub();
					assertThat( sub ).isNotNull();
					assertThat( Hibernate.isInitialized( sub ) ).isTrue();
					assertThat( connector.getSub() ).isSameAs( sub );
					assertThat( sub.getConnector() ).isSameAs( connector );
				}
		);
	}

	@Test
	public void testQuery2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Main> result = session.createQuery(
							"select m from Main m where m.connector.id = 1",
							Main.class
					).getResultList();

					assertThat( result.size() ).isEqualTo( 1 );
					Main main = result.get( 0 );
					Connector connector = main.getConnector();
					assertThat( Hibernate.isInitialized( connector ) ).isTrue();
					assertThat( connector ).isNotNull();
					Sub sub = main.getSub();
					assertThat( sub ).isNotNull();
					assertThat( Hibernate.isInitialized( sub ) ).isTrue();
					assertThat( connector.getSub() ).isSameAs( sub );
					assertThat( sub.getConnector() ).isSameAs( connector );
				}
		);
	}


	@Entity(name = "Main")
	public static class Main {

		@Id
		private Long id;

		private String name;

		@ManyToOne
		private Connector connector;

		@ManyToOne(fetch = FetchType.LAZY)
		private Sub sub;

		public Main() {
		}

		public Main(Long id, String name, Connector connector, Sub sub) {
			this.id = id;
			this.name = name;
			this.connector = connector;
			this.sub = sub;
		}

		public Connector getConnector() {
			return connector;
		}

		public void setConnector(Connector connector) {
			this.connector = connector;
		}

		public Sub getSub() {
			return sub;
		}

		public void setSub(Sub sub) {
			this.sub = sub;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}
	}


	@Entity(name = "Connector")
	public static class Connector {

		@Id
		private Long id;

		private String name;

		@ManyToOne
		@Fetch(FetchMode.SELECT)
		private Sub sub;

		public Connector() {
		}

		public Connector(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Sub getSub() {
			return sub;
		}

		public void setSub(Sub sub) {
			this.sub = sub;
		}

		public Long getId() {
			return id;
		}
	}

	@Entity(name = "Sub")
	public static class Sub {

		@Id
		private Long id;

		private String name;

		@ManyToOne
		private Connector connector;

		public Sub() {
		}

		public Sub(Long id, String name, Connector connector) {
			this.id = id;
			this.name = name;
			this.connector = connector;
			connector.setSub( this );
		}

		public Long getId() {
			return id;
		}

		public Connector getConnector() {
			return connector;
		}

		public void setConnector(Connector connector) {
			this.connector = connector;
		}
	}


}
