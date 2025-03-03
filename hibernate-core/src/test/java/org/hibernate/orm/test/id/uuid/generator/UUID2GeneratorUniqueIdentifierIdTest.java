/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.uuid.generator;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect( SQLServerDialect.class )
@JiraKey( "HHH-12943" )
@DomainModel(annotatedClasses = { UUID2GeneratorUniqueIdentifierIdTest.FooEntity.class })
@SessionFactory
public class UUID2GeneratorUniqueIdentifierIdTest {

	@Test
	public void testPaginationQuery(SessionFactoryScope scope) {
		final UUID id = scope.fromTransaction( session -> {
			final FooEntity entity = new FooEntity();
			entity.fooValues.add("one");
			entity.fooValues.add("two");
			entity.fooValues.add("three");
			session.persist( entity );
			return entity.id;
		} );

		assertThat( id, notNullValue() );

		scope.inTransaction( session -> {
			final FooEntity entity = session.find(FooEntity.class, id);
			assertThat( entity, notNullValue() );
			assertThat( entity.fooValues, hasSize( 3 ) );
		} );
	}

	@Entity(name = "FootEntity")
	static class FooEntity {

		@Id
		@GenericGenerator(name = "uuid", strategy = "uuid2")
		@GeneratedValue(generator = "uuid")
		@Column(columnDefinition = "UNIQUEIDENTIFIER")
		UUID id;

		@ElementCollection
		@JoinTable(name = "foo_values")
		@Column(name = "foo_value")
		final Set<String> fooValues = new HashSet<>();

	}
}
