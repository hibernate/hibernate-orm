/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.orphan;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;


/**
 * @author Steve Ebersole
 */
@JiraKey(value = "HHH-565")
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/orphan/User.hbm.xml",
				"org/hibernate/orm/test/orphan/Mail.hbm.xml"
		}
)
@SessionFactory
public class PropertyRefTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testDeleteParentWithBidirOrphanDeleteCollectionBasedOnPropertyRef(SessionFactoryScope scope) {
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
					User u = session.getReference( User.class, user.getId() );
					session.remove( u );
				}
		);

		scope.inTransaction(
				session -> {
					session.createQuery( "delete from Mail where alias = :alias" )
							.setParameter( "alias", "test" )
							.executeUpdate();
					session.createQuery( "delete from User where userid = :userid" )
							.setParameter( "userid", "test" )
							.executeUpdate();
				}
		);
	}

}
