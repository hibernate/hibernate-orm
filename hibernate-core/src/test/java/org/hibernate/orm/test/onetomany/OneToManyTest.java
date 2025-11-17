/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetomany;


import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/onetomany/Parent.hbm.xml"
)
@SessionFactory
public class OneToManyTest {

	@SuppressWarnings("unchecked")
	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTemporaryTable.class)
	public void testOneToManyLinkTable(SessionFactoryScope scope) {
		Child c = new Child();
		Parent p = new Parent();
		scope.inTransaction(
				session -> {
					c.setName( "Child One" );
					p.setName( "Parent" );
					p.getChildren().add( c );
					c.setParent( p );
					session.persist( p );
					session.flush();

					p.getChildren().remove( c );
					c.setParent( null );
					session.flush();

					p.getChildren().add( c );
					c.setParent( p );
				}
		);

		@SuppressWarnings("unused")
		Child merged = scope.fromTransaction(
				session -> {
					c.setParent( null );
					return session.merge( c );

				}
		);

		scope.inTransaction(
				session -> {
					merged.setParent( p );
					session.merge( merged );
				}
		);


		scope.inTransaction(
				session -> {
					@SuppressWarnings("unused")
					Child child = (Child) session.createQuery( "from Child" ).uniqueResult();
					session.createQuery( "from Child c left join fetch c.parent" ).list();
					session.createQuery( "from Child c inner join fetch c.parent" ).list();
					session.clear();
					session.createQuery( "from Parent p left join fetch p.children" )
							.uniqueResult();
				}
		);

		scope.inTransaction(
				session -> {
					session.createQuery( "delete from Child" ).executeUpdate();
					session.createQuery( "delete from Parent" ).executeUpdate();
				}
		);
	}

	@Test
	public void testManyToManySize(SessionFactoryScope scope) {

		scope.inTransaction(
				session ->
						assertEquals(
								0,
								session.createQuery( "from Parent p where size(p.children) = 0" ).list().size()
						)
		);
	}

}
