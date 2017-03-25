/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.inheritance.discriminator.joinedsubclass;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
public class JoinedSubclassWithRootInterfaceTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String[] getMappings() {
		return new String[] {"TestEntity.hbm.xml"};
	}

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/test/inheritance/discriminator/joinedsubclass/";
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11554")
	public void testIt() {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			final TestEntityImpl testEntity = new TestEntityImpl();
			testEntity.setId( 1 );
			session.save( testEntity );
		} );
	}
}
