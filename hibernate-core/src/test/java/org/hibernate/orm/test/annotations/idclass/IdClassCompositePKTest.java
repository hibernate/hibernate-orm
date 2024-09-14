/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.idclass;


import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Stale W. Pedersen
 */
@DomainModel(annotatedClasses = DomainAdmin.class)
@SessionFactory
public class IdClassCompositePKTest {

	@Test
	public void testEntityMappningPropertiesAreNotIgnored(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					DomainAdmin da = new DomainAdmin();
					da.setAdminUser( "admin" );
					da.setDomainName( "org" );

					session.persist( da );
					Query q = session.getNamedQuery( "DomainAdmin.testQuery" );
					assertEquals( 1, q.list().size() );
				}
		);
	}

}
