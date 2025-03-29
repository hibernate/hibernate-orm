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

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

import org.hibernate.orm.test.hql.SimpleEntityWithAssociation;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Etienne Miret
 */
public class NativeCriteriaSyncTest extends BaseCoreFunctionalTestCase {

	/**
	 * Tests that the join table of a many-to-many relationship is properly flushed before making a related Criteria
	 * query.
	 */
	@Test
	@JiraKey( value = "HHH-3813" )
	public void test() {


		final SimpleEntityWithAssociation e1 = new SimpleEntityWithAssociation( "e1" );
		final SimpleEntityWithAssociation e2 = new SimpleEntityWithAssociation( "e2" );
		e1.getManyToManyAssociatedEntities().add( e2 );

		doInHibernate( this::sessionFactory, session -> {
			session.persist( e1 );

			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<SimpleEntityWithAssociation> criteria = criteriaBuilder.createQuery( SimpleEntityWithAssociation.class );
			Root<SimpleEntityWithAssociation> root = criteria.from( SimpleEntityWithAssociation.class );
			Join<Object, Object> join = root.join(
					"manyToManyAssociatedEntities",
					JoinType.INNER
			);
			criteria.where( criteriaBuilder.equal( join.get( "name" ), "e2" ) );

			assertEquals(1, session.createQuery( criteria ).list().size());

//			final Criteria criteria = session.createCriteria( SimpleEntityWithAssociation.class );
//			criteria.createCriteria( "manyToManyAssociatedEntities" ).add( Restrictions.eq( "name", "e2" ) );
//			assertEquals( 1, criteria.list().size() );
		} );
	}

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
	protected String[] getMappings() {
		return new String[] { "hql/SimpleEntityWithAssociation.hbm.xml" };
	}

}
