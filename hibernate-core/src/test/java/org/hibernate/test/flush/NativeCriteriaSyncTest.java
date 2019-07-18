/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.flush;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

import org.hibernate.test.hql.SimpleEntityWithAssociation;
import org.hibernate.testing.TestForIssue;
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
	@TestForIssue( jiraKey = "HHH-3813" )
	public void test() {


		final SimpleEntityWithAssociation e1 = new SimpleEntityWithAssociation( "e1" );
		final SimpleEntityWithAssociation e2 = new SimpleEntityWithAssociation( "e2" );
		e1.getManyToManyAssociatedEntities().add( e2 );

		doInHibernate( this::sessionFactory, session -> {
			session.save( e1 );

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
	protected String[] getMappings() {
		return new String[] { "hql/SimpleEntityWithAssociation.hbm.xml" };
	}

}
