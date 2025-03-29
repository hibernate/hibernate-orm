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
import jakarta.persistence.MappedSuperclass;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@JiraKey("HHH-17064")
@DomainModel(
		annotatedClasses = {
				ManyToOneWithCircularity3Test.Main.class,
				ManyToOneWithCircularity3Test.Connector.class,
				ManyToOneWithCircularity3Test.Sub.class,
				ManyToOneWithCircularity3Test.Sub2.class,
		}
)
@SessionFactory
public class ManyToOneWithCircularity3Test {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Connector connector = new Connector( 1L, "connector" );
					Sub sub = new Sub( 2L, "sub", connector );
					Sub2 sub2 = new Sub2( 2L, "sub2", connector );
					Main main = new Main( 3L, "main", connector, sub, sub2 );

					session.persist( sub );
					session.persist( sub2 );
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
					assertThat( connector.getSub2() ).isNotNull();
					assertThat( Hibernate.isInitialized(connector.getSub2()) ).isTrue();
					assertThat( ((Sub2)connector.getSub2()).getConnector() ).isSameAs( connector );
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
					assertThat( connector.getSub2() ).isNotNull();
					assertThat( Hibernate.isInitialized(connector.getSub2()) ).isTrue();
					assertThat( ((Sub2)connector.getSub2()).getConnector() ).isSameAs( connector );
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

		@ManyToOne(fetch = FetchType.LAZY)
		private Sub2 sub2;

		public Main() {
		}

		public Main(Long id, String name, Connector connector, Sub sub, Sub2 sub2) {
			this.id = id;
			this.name = name;
			this.connector = connector;
			this.sub = sub;
			this.sub2 = sub2;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Connector getConnector() {
			return connector;
		}

		public Sub getSub() {
			return sub;
		}

		public Sub2 getSub2() {
			return sub2;
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

		@ManyToOne
		@Fetch(FetchMode.SELECT)
		private Sub2 sub2;

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

		public SubParent getSub2() {
			return sub2;
		}

		public void setSub2(Sub2 sub2) {
			this.sub2 = sub2;
		}

		public Long getId() {
			return id;
		}
	}

	@MappedSuperclass
	public static class SubParent {
		@Id
		private Long id;

		public SubParent() {
		}

		public SubParent(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

	}

	@Entity(name = "Sub")
	public static class Sub extends SubParent {

		private String name;

		@ManyToOne
		private Connector connector;

		public Sub() {
		}

		public Sub(Long id, String name, Connector connector) {
			super( id );
			this.name = name;
			this.connector = connector;
			connector.setSub( this );
		}


		public Connector getConnector() {
			return connector;
		}

		public void setConnector(Connector connector) {
			this.connector = connector;
		}
	}

	@Entity(name = "Sub2")
	public static class Sub2 extends SubParent {

		private String name;

		@ManyToOne
		private Connector connector;

		public Sub2() {
		}

		public Sub2(Long id, String name, Connector connector) {
			super( id );
			this.name = name;
			this.connector = connector;
			connector.setSub2( this );
		}


		public Connector getConnector() {
			return connector;
		}

		public void setConnector(Connector connector) {
			this.connector = connector;
		}
	}


}
