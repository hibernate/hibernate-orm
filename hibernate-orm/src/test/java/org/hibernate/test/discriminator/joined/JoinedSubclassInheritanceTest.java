/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.discriminator.joined;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11133")
public class JoinedSubclassInheritanceTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String[] getMappings() {
		return new String[] {"discriminator/joined/JoinedSubclassInheritance.hbm.xml"};
	}

	@Test
	public void testConfiguredDiscriminatorValue() {
		final ChildEntity childEntity = new ChildEntity( 1, "Child" );
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.save( childEntity );
		} );

		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			ChildEntity ce = session.find( ChildEntity.class, 1 );
			assertEquals( "ce", ce.getType() );
		} );
	}

}
