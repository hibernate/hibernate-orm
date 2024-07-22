/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.collection.propertyref;

import java.util.Set;

import org.hibernate.Hibernate;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Steve Ebersole
 */
@TestForIssue(jiraKey = "HHH-565")
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/collection/propertyref/User.hbm.xml",
				"org/hibernate/orm/test/collection/propertyref/Mail.hbm.xml"
		}
)
@SessionFactory
public class PropertyRefTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from Mail" ).executeUpdate();
					session.createQuery( "delete from User" ).executeUpdate();
				}
		);
	}

	@Test
	public void testGet(SessionFactoryScope scope){
		User user = new User( "test" );
		scope.inTransaction(
				session -> {
					user.addMail( "test" );
					user.addMail( "test" );
					session.persist( user );
				}
		);

		scope.inTransaction(
				session -> {
					final User u = session.get( User.class, user.getId() );

					final Set<Mail> mail = u.getMail();
					assertTrue( Hibernate.isInitialized( mail ) );
					assertEquals( 2, mail.size() );

					final String alias = mail.iterator().next().getAlias();
					assertEquals( "test", alias );
				}
		);
	}


}
