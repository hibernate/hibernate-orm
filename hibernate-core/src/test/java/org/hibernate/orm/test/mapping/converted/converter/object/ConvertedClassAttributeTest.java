/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.object;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		ConvertedClassAttributeTest.EntityWithStatus.class,
		Status.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-18564" )
public class ConvertedClassAttributeTest {
	@Test
	public void testConvertedAttributeSelection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var resultList = session.createQuery(
					"select t.status from EntityWithStatus t",
					Status.class
			).getResultList();
			assertThat( resultList ).hasSize( 2 ).containsExactlyInAnyOrder( Status.ONE, Status.TWO );
		} );
	}

	@Test
	public void testLiteralPredicateDomainForm(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var result = session.createQuery(
					String.format( "select t from EntityWithStatus t where t.status > %s.ONE", Status.class.getName() ),
					EntityWithStatus.class
			).getSingleResult();
			assertThat( result.getStatus().getValue() ).isEqualTo( 2 );
		} );
	}

	@Test
	public void testLiteralPredicateRelationalForm(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var result = session.createQuery(
					"select t from EntityWithStatus t where t.status > 1",
					EntityWithStatus.class
			).getSingleResult();
			assertThat( result.getStatus().getValue() ).isEqualTo( 2 );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityWithStatus entity1 = new EntityWithStatus();
			entity1.setStatus( Status.ONE );
			session.persist( entity1 );
			final EntityWithStatus entity2 = new EntityWithStatus();
			entity2.setStatus( Status.TWO );
			session.persist( entity2 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity( name = "EntityWithStatus" )
	static class EntityWithStatus {
		@Id
		@GeneratedValue
		private Long id;

		@Convert( converter = StatusConverter.class )
		private Status status;

		public Long getId() {
			return id;
		}

		public Status getStatus() {
			return status;
		}

		public void setStatus(Status status) {
			this.status = status;
		}
	}

	@Converter
	static class StatusConverter implements AttributeConverter<Status, Integer> {
		@Override
		public Integer convertToDatabaseColumn(Status attribute) {
			return attribute == null ? null : attribute.getValue();
		}

		@Override
		public Status convertToEntityAttribute(Integer dbData) {
			return dbData == null ? null : Status.from( dbData );
		}
	}

}
