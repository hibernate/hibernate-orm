/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.naturalid.idclass;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.FindBy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {
		SimpleNaturalIdClassTests.Key.class,
		SimpleNaturalIdClassTests.TestEntity.class,
		SimpleNaturalIdClassTests.TestEntity2.class
})
@Jira( "https://hibernate.atlassian.net/browse/HHH-16383" )
@SessionFactory
public class SimpleNaturalIdClassTests {
	@BeforeEach
	void setUp(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.persist( new TestEntity( 1, "steve", "ebersole" ) );
			session.persist( new TestEntity2( 1, "steve" ) );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	void testBootModel(DomainModelScope modelScope) {
		modelScope.getDomainModel().validate();
		final RootClass entity1Binding = (RootClass) modelScope.getEntityBinding( TestEntity.class );
		assertEquals( Key.class, entity1Binding.getNaturalIdClass().toJavaClass() );
		final RootClass entity2Binding = (RootClass) modelScope.getEntityBinding( TestEntity2.class );
		assertNull( entity2Binding.getNaturalIdClass() );
	}

	@Test
	void findBySimpleSmokeTest(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var result = session.findByNaturalId( TestEntity2.class, "steve" );
			assertEquals( 1, result.id );
		} );
	}

	@Test
	void findMultipleBySimpleSmokeTest(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var results = session.findMultipleByNaturalId( TestEntity2.class, List.of( "steve", "john" ) );
			assertThat( results ).hasSize( 2 );
			assertEquals( 1,  results.get( 0 ).id );
			assertNull( results.get( 1 ) );
		} );
	}

	@Test
	void findByClassSmokeTest(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var result = session.findByNaturalId( TestEntity.class, new Key("steve", "ebersole") );
			assertEquals( 1, result.id );
		} );
	}

	@Test
	void findByClassFindBySmokeTest(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var result = session.find( TestEntity.class, new Key("steve", "ebersole"), FindBy.NATURAL_ID );
			assertEquals( 1, result.id );
		} );
	}

	@Test
	void findMultipleByClassSmokeTest(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var results = session.findMultipleByNaturalId( TestEntity.class, List.of( new Key("steve", "ebersole") ) );
			assertThat( results ).hasSize( 1 );
			assertEquals( 1,  results.get( 0 ).id );
		} );
	}

	@Test
	void findMultipleByClassFindBySmokeTest(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var results = session.findMultiple( TestEntity.class, List.of( new Key("steve", "ebersole") ), FindBy.NATURAL_ID );
			assertThat( results ).hasSize( 1 );
			assertEquals( 1,  results.get( 0 ).id );
		} );
	}

	@Entity(name="TestEntity")
	@Table(name="TestEntity")
	@NaturalIdClass(Key.class)
	public static class TestEntity {
		@Id
		private Integer id;
		@NaturalId
		private String name1;
		@NaturalId
		private String name2;

		public TestEntity() {
		}

		public TestEntity(Integer id, String name1, String name2) {
			this.id = id;
			this.name1 = name1;
			this.name2 = name2;
		}
	}

	public record Key(String name1, String name2) {
	}

	@Entity(name="TestEntity2")
	@Table(name="TestEntity2")
	public static class TestEntity2 {
		@Id
		private Integer id;
		@NaturalId
		private String name;

		public TestEntity2() {
		}

		public TestEntity2(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
