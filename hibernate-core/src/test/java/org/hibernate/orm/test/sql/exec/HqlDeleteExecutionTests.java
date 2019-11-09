/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.sql.exec;

import org.hibernate.orm.test.metamodel.mapping.PluralAttributeTests;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
@DomainModel( standardModels = StandardDomainModel.GAMBIT )
@ServiceRegistry
@SessionFactory( exportSchema = true )
public class HqlDeleteExecutionTests {
	@Test
	public void testSimpleDelete(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createQuery( "delete BasicEntity" ).executeUpdate()
		);
	}

	@Test
	public void testSimpleRestrictedDelete(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createQuery( "delete BasicEntity where data = :filter" )
						.setParameter( "filter", "abc" )
						.executeUpdate()
		);
	}
}
