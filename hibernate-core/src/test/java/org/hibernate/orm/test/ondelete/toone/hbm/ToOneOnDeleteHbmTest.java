/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ondelete.toone.hbm;

import org.hibernate.dialect.SybaseDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;


/**
 * @author Vlad Mihalcea
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/ondelete/toone/hbm/ToOneOnDelete.hbm.xml"
)
@SessionFactory
public class ToOneOnDeleteHbmTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope){
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@SkipForDialect(
			dialectClass = SybaseDialect.class,
			matchSubTypes = true,
			reason = "HHH-13559 on-delete=\"cascade\" is not supported for unidirectional to-one associations using Sybase"
	)
	public void testManyToOne(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = new Parent();
					parent.setId( 1L );
					session.persist( parent );

					Child child1 = new Child();
					child1.setId( 1L );
					child1.setParent( parent );
					session.persist( child1 );

					GrandChild grandChild11 = new GrandChild();
					grandChild11.setId( 1L );
					grandChild11.setParent( child1 );
					session.persist( grandChild11 );

					Child child2 = new Child();
					child2.setId( 2L );
					child2.setParent( parent );
					session.persist( child2 );

					GrandChild grandChild21 = new GrandChild();
					grandChild21.setId( 2L );
					grandChild21.setParent( child2 );
					session.persist( grandChild21 );

				}
		);


		scope.inTransaction(
				session -> {
					Parent parent = session.get( Parent.class, 1L );
					session.remove( parent );
				}
		);
	}

}
