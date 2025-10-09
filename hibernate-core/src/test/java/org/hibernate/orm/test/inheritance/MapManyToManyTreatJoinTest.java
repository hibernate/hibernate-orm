/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel( annotatedClasses = {
		MapManyToManyTreatJoinTest.JoinedBase.class,
		MapManyToManyTreatJoinTest.JoinedSub1.class,
		MapManyToManyTreatJoinTest.JoinedSub2.class
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17255" )
public class MapManyToManyTreatJoinTest {
	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final JoinedSub1 o1 = new JoinedSub1( 1, 123 );
			final JoinedSub2 o2 = new JoinedSub2( 2, "321" );
			session.persist( o2 );
			session.persist( o1 );
			o1.getSubMap().put( 2, o2 );
			o2.getSubMap().put( 1, o1 );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testTreatQueryStringProp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final String result = session.createSelectionQuery(
					"select c.stringProp from JoinedBase t join treat(t.subMap as JoinedSub2) c",
					String.class
			).getSingleResult();
			assertThat( result ).isEqualTo( "321" );
		} );
	}

	@Test
	public void testTreatQueryIntProp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Integer result = session.createSelectionQuery(
					"select c.intProp from JoinedBase t join treat(t.subMap as JoinedSub1) c",
					Integer.class
			).getSingleResult();
			assertThat( result ).isEqualTo( 123 );
		} );
	}

	@SuppressWarnings({"FieldCanBeLocal", "unused", "FieldMayBeFinal"})
	@Entity( name = "JoinedBase" )
	@Inheritance( strategy = InheritanceType.JOINED )
	public static abstract class JoinedBase {
		@Id
		private Integer id;

		private String name;

		@ManyToMany
		@JoinTable( name = "sub_map" )
		private Map<Integer, JoinedBase> subMap = new HashMap<>();

		public JoinedBase() {
		}

		public JoinedBase(Integer id) {
			this.id = id;
		}

		public Map<Integer, JoinedBase> getSubMap() {
			return subMap;
		}
	}

	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	@Entity( name = "JoinedSub1" )
	@Table( name = "joined_sub_1" )
	public static class JoinedSub1 extends JoinedBase {
		private Integer intProp;

		public JoinedSub1() {
		}

		public JoinedSub1(Integer id, Integer intProp) {
			super( id );
			this.intProp = intProp;
		}
	}

	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	@Entity( name = "JoinedSub2" )
	@Table( name = "joined_sub_2" )
	public static class JoinedSub2 extends JoinedBase {
		private String stringProp;

		public JoinedSub2() {
		}

		public JoinedSub2(Integer id, String stringProp) {
			super( id );
			this.stringProp = stringProp;
		}
	}
}
