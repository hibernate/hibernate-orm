/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.inheritance.discriminator;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Pawel Stawicki
 */
@RequiresDialect( value = {PostgreSQL81Dialect.class, PostgreSQLDialect.class}, jiraKey = "HHH-6580" )
public class PersistChildEntitiesWithDiscriminatorTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { ParentEntity.class, InheritingEntity.class };
	}

	@Test
	public void doIt() {
		Session session = openSession();
		session.beginTransaction();
		// we need the 2 inserts so that the id is incremented on the second get-generated-keys-result set, since
		// on the first insert both the pk and the discriminator values are 1
		session.save( new InheritingEntity( "yabba" ) );
		session.save( new InheritingEntity( "dabba" ) );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		session.createQuery( "delete ParentEntity" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

}
