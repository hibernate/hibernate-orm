/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.discriminator.joinedsubclass;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-9302")
@DomainModel(
		annotatedClasses = {
				RootEntity.class, SubEntity.class, SubSubEntity.class, SubSubSubEntity.class
		}
)
@SessionFactory
public class JoinedSubclassTest {

	private Long subSubEntityId;
	private static final String EXPECTED_SUB_SUB_STRING_VALUE = "SubSub";

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		final SubSubEntity subSubEntity = new SubSubEntity();
		final SubEntity subEntity = new SubSubEntity();
		subSubEntity.setSubSubString( EXPECTED_SUB_SUB_STRING_VALUE );
		scope.inTransaction(
				session -> {
					session.persist( subEntity );
					session.persist( subSubEntity );
				}
		);
		subSubEntityId = subSubEntity.getId();
	}

	@Test
	public void shouldRetrieveSubEntity(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					RootEntity loaded = session.get( SubEntity.class, subSubEntityId );
					assertNotNull( loaded );
					assertTrue( loaded instanceof SubSubEntity );
					assertEquals( EXPECTED_SUB_SUB_STRING_VALUE, ( (SubSubEntity) loaded ).getSubSubString() );
				}
		);
	}

	@Test
	public void shouldNotRetrieveSubSubSubEntity(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					SubSubSubEntity loaded = session.get( SubSubSubEntity.class, subSubEntityId );
					assertNull( loaded );
				}
		);
	}

	// Criteria

	@Test
	public void shouldRetrieveSubSubEntityWithCriteria(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					final CriteriaBuilder criteriaBuilder = session.getSessionFactory().getCriteriaBuilder();
					final CriteriaQuery<SubSubEntity> criteria = criteriaBuilder.createQuery( SubSubEntity.class );
					final Root<SubSubEntity> root = criteria.from( SubSubEntity.class );
					criteria.where( criteriaBuilder.equal( root.get( SubSubEntity_.id ), subSubEntityId ) );
					final SubSubEntity loaded = session.createQuery( criteria ).uniqueResult();
					assertNotNull( loaded );
				}
		);
	}

	@Test
	public void shouldNotRetrieveSubSubSubEntityWithCriteria(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					final CriteriaBuilder criteriaBuilder = session.getSessionFactory().getCriteriaBuilder();
					final CriteriaQuery<SubSubSubEntity> criteria = criteriaBuilder.createQuery( SubSubSubEntity.class );
					final Root<SubSubSubEntity> root = criteria.from( SubSubSubEntity.class );

					criteria.where( criteriaBuilder.equal( root.get( SubSubSubEntity_.id ), subSubEntityId ) );
					final SubSubEntity loaded = session.createQuery( criteria ).uniqueResult();
					assertNull( loaded );
				}
		);
	}

	// HQL

	@Test
	public void shouldRetrieveSubSubEntityWithHQL(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					SubSubEntity loaded = (SubSubEntity) session.createQuery(
							"select se from SubSubEntity se where se.id = :id" )
							.setParameter( "id", subSubEntityId )
							.uniqueResult();
					assertNotNull( loaded );
				}
		);
	}

	@Test
	public void shouldNotRetrieveSubSubSubEntityWithHQL(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					SubSubSubEntity loaded = (SubSubSubEntity) session.createQuery(
							"select se from SubSubSubEntity se where se.id = :id" )
							.setParameter( "id", subSubEntityId )
							.uniqueResult();
					assertNull( loaded );
				}
		);
	}

}
