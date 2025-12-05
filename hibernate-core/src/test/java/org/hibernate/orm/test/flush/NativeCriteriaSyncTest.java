/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.flush;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import org.hibernate.orm.test.hql.SimpleEntityWithAssociation;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Etienne Miret
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey("HHH-3813")
@DomainModel(xmlMappings = "org/hibernate/orm/test/hql/SimpleEntityWithAssociation.hbm.xml")
@SessionFactory
public class NativeCriteriaSyncTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	/**
	 * Tests that the join table of a many-to-many relationship is properly flushed before making a related Criteria
	 * query.
	 */
	@Test
	public void test(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final SimpleEntityWithAssociation e1 = new SimpleEntityWithAssociation( "e1" );
			final SimpleEntityWithAssociation e2 = new SimpleEntityWithAssociation( "e2" );
			e1.getManyToManyAssociatedEntities().add( e2 );
			session.persist( e1 );
		} );

		factoryScope.inTransaction( (session) -> {
			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<SimpleEntityWithAssociation> criteria = criteriaBuilder.createQuery( SimpleEntityWithAssociation.class );
			Root<SimpleEntityWithAssociation> root = criteria.from( SimpleEntityWithAssociation.class );
			Join<Object, Object> join = root.join(
					"manyToManyAssociatedEntities",
					JoinType.INNER
			);
			criteria.where( criteriaBuilder.equal( join.get( "name" ), "e2" ) );

			assertEquals( 1, session.createQuery( criteria ).list().size() );
		} );
	}

}
