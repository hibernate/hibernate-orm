/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.optimizer;

import org.hibernate.orm.test.bytecode.enhancement.optimizer.child.ChildEntity10;
import org.hibernate.orm.test.bytecode.enhancement.optimizer.parent.Ancestor;
import org.hibernate.orm.test.bytecode.enhancement.optimizer.parent.ChildEntity2;
import org.hibernate.query.Query;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		Ancestor.class,
		AncestorEntity.class,
		ChildEntity2.class,
		ChildEntity10.class
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-19372")
@BytecodeEnhanced
public class HierarchyBytecodeOptimizerMethodVisibilityTest {

	@Test
	public void testOptimizerSetPropertyValues(SessionFactoryScope scope) {
		ChildEntity2 childEntity2 = new ChildEntity2();
		childEntity2.setId( 1L );
		childEntity2.setField( "field" );
		childEntity2.setChieldField( "childField" );

		ChildEntity10 childEntity10 = new ChildEntity10();
		childEntity10.setId( 3L );
		childEntity10.setField( "field10" );
		childEntity10.setChieldField( "childField3" );

		scope.inTransaction( session -> {
			session.persist( childEntity2 );
			session.persist( childEntity10 );
		} );

		scope.inTransaction( session -> {
			Query<ChildEntity2> query = session.createQuery( "select c from ChildEntity2 c where c.field = :field",
					ChildEntity2.class );
			query.setParameter( "field", "field" );
			assertThat( query.uniqueResult() ).isNotNull();
		} );

		scope.inTransaction( session -> {
			Query<ChildEntity10> query = session.createQuery( "select c from ChildEntity10 c where c.field = :field",
					ChildEntity10.class );
			query.setParameter( "field", "field10" );
			assertThat( query.uniqueResult() ).isNotNull();
		} );
	}

	@AfterAll
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}
}
