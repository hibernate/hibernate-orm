/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.naturalid.idclass;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;

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
public class SimpleNaturalIdClassTests {
	@Test
	void testBootModel(DomainModelScope modelScope) {
		modelScope.getDomainModel().validate();
		final RootClass entity1Binding = (RootClass) modelScope.getEntityBinding( TestEntity.class );
		assertEquals( Key.class, entity1Binding.getNaturalIdClass().toJavaClass() );
		final RootClass entity2Binding = (RootClass) modelScope.getEntityBinding( TestEntity2.class );
		assertNull( entity2Binding.getNaturalIdClass() );
	}

	@Test
	@SessionFactory
	void findBySimpleSmokeTest(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.findByNaturalId( TestEntity2.class, "steve" );
		} );
	}

	@Test
	@SessionFactory
	void findMultipleBySimpleSmokeTest(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.findMultipleByNaturalId( TestEntity2.class, List.of( "steve", "john" ) );
		} );
	}

	@Test
	@SessionFactory
	void findByClassSmokeTest(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.findByNaturalId( TestEntity.class, new Key("steve", "ebersole") );
		} );
	}

	@Test
	@SessionFactory
	void findMultipleByClassSmokeTest(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.findMultipleByNaturalId( TestEntity.class, List.of( new Key("steve", "ebersole") ) );
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
	}
}
