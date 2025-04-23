/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.optimizer;

import org.hibernate.orm.test.bytecode.enhancement.optimizer.parent.Ancestor;
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
		ParentEntity.class,
		ChildEntity3.class,
		ChildEntity4.class,
		ChildEntity5.class,
		ChildEntity6.class,
		ChildEntity7.class,
		ChildEntity8.class,
		ChildEntity9.class
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-19369")
@BytecodeEnhanced
public class HierarchyBytecodeOptimizerOrderingTest {

	@Test
	public void testOptimizerSetPropertyValues(SessionFactoryScope scope) {
		ChildEntity3 childEntity3 = new ChildEntity3();
		childEntity3.setId( 3L );
		childEntity3.setName( "child3" );
		childEntity3.setField( "field3" );
		childEntity3.setChieldField( "childField3" );

		ChildEntity4 childEntity4 = new ChildEntity4();
		childEntity4.setId( 4L );
		childEntity4.setName( "child4" );
		childEntity4.setField( "field4" );
		childEntity4.setChieldField( "childField4" );

		ChildEntity5 childEntity5 = new ChildEntity5();
		childEntity5.setId( 5L );
		childEntity5.setName( "child5" );
		childEntity5.setField( "field5" );
		childEntity5.setChieldField( "childField5" );

		ChildEntity6 childEntity6 = new ChildEntity6();
		childEntity6.setId( 6L );
		childEntity6.setName( "child6" );
		childEntity6.setField( "field6" );
		childEntity6.setChieldField( "childField6" );

		ChildEntity7 childEntity7 = new ChildEntity7();
		childEntity7.setId( 7L );
		childEntity7.setName( "child7" );
		childEntity7.setField( "field7" );
		childEntity7.setChildField( "childField7" );

		scope.inTransaction( session -> {
			session.persist( childEntity3 );
			session.persist( childEntity4 );
			session.persist( childEntity5 );
			session.persist( childEntity6 );
			session.persist( childEntity7 );
		} );

		scope.inTransaction( session -> {
			Query<ChildEntity3> query = session.createQuery( "select c from ChildEntity3 c where c.field = :field",
					ChildEntity3.class );
			query.setParameter( "field", "field3" );
			assertThat( query.uniqueResult() ).isNotNull();
		} );

		scope.inTransaction( session -> {
			Query<ChildEntity4> query = session.createQuery( "select c from ChildEntity4 c where c.field = :field",
					ChildEntity4.class );
			query.setParameter( "field", "field4" );
			assertThat( query.uniqueResult() ).isNotNull();
		} );

		scope.inTransaction( session -> {
			Query<ChildEntity5> query = session.createQuery( "select c from ChildEntity5 c where c.field = :field",
					ChildEntity5.class );
			query.setParameter( "field", "field5" );
			assertThat( query.uniqueResult() ).isNotNull();
		} );

		scope.inTransaction( session -> {
			Query<ChildEntity6> query = session.createQuery( "select c from ChildEntity6 c where c.field = :field",
					ChildEntity6.class );
			query.setParameter( "field", "field6" );
			assertThat( query.uniqueResult() ).isNotNull();
		} );

		scope.inTransaction( session -> {
			Query<ChildEntity7> query = session.createQuery(
					"select c from ChildEntity7 c where c.field = :field", ChildEntity7.class );
			query.setParameter( "field", "field7" );
			assertThat( query.uniqueResult() ).isNotNull();
		} );
	}

	@AfterAll
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}
}
