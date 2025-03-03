/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component.genericinheritance;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		UserEntity.class,
		ExampleSuperClassEmbedded.class,
		ExampleEmbedded.class,
		ExampleEntity.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16755" )
public class EmbeddableGenericsAndInterfaceTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final UserEntity user = new UserEntity();
			user.setName( "Debbie" );
			session.persist( user );
			final ExampleEmbedded<?> embedded = new ExampleEmbedded<>();
			embedded.setUser( user );
			final ExampleEntity entity = new ExampleEntity();
			entity.setExampleEmbedded( embedded );
			session.persist( entity );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from ExampleEntity" ).executeUpdate();
			session.createMutationQuery( "delete from UserEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testMetamodelCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<ExampleEntity> cq = cb.createQuery( ExampleEntity.class );
			final Root<ExampleEntity> root = cq.from( ExampleEntity.class );
			cq.select( root ).where( cb.isNotNull(
					root.get( ExampleEntity_.exampleEmbedded ).get( ExampleEmbedded_.user )
			) );
			final ExampleEntity result = session.createQuery( cq ).getSingleResult();
			assertThat( result.getExampleEmbedded().getUser().getName() ).isEqualTo( "Debbie" );
		} );
	}
}
