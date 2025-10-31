/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql.mutation;

import from.In;
import in.from.Any;
import org.hibernate.query.Query;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-10953")
@DomainModel(
		annotatedClasses = {
				Any.class, In.class
		}
)
@SessionFactory
public class UpdateEntitiesWithPackageNamesStartingWithKeywordsTest {


	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testUpdateEntityWithPackageNameStartingWithIn(SessionFactoryScope scope) {
		Any entity = new Any();
		entity.setProp( "1" );

		scope.inTransaction(
				session -> session.persist( entity )
		);

		scope.inTransaction(
				session -> {
					final Query query = session.createQuery( "UPDATE Any set prop = :prop WHERE id = :id " );
					query.setParameter( "prop", "1" );
					query.setParameter( "id", entity.getId() );
					query.executeUpdate();
				}
		);
		scope.inTransaction(
				session -> session.createQuery( "DELETE FROM Any" ).executeUpdate()
		);
	}

	@Test
	public void testUpdateEntityWithPackageNameStartingWithFrom(SessionFactoryScope scope) {
		In entity = new In();
		entity.setProp( "1" );
		scope.inTransaction(
				session -> session.persist( entity )
		);
		scope.inTransaction(
				session -> {
					final Query query = session.createQuery( "UPDATE In set prop = :prop WHERE id = :id " );
					query.setParameter( "prop", "1" );
					query.setParameter( "id", entity.getId() );
					query.executeUpdate();
				}
		);

		scope.inTransaction(
				session -> session.createQuery( "DELETE FROM In" ).executeUpdate()
		);
	}
}
