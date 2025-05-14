/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.optimizer;

import org.hibernate.orm.test.bytecode.enhancement.optimizer.child.ChildEntity;
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
		ParentEntity.class,
		ChildEntity.class,
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-19372")
@BytecodeEnhanced
public class HierarchyBytecodeOptimizerTest {

	@Test
	public void testOptimizerSetPropertyValues(SessionFactoryScope scope) {
		ChildEntity childEntity = new ChildEntity();
		childEntity.setId( 1L );
		childEntity.setField( "field" );
		childEntity.setChieldField( "childField" );

		scope.inTransaction( session -> {
			session.persist( childEntity );
		} );

		scope.inTransaction( session -> {
			Query<ChildEntity> query = session.createQuery( "select c from ChildEntity c where c.field = :field",
					ChildEntity.class );
			query.setParameter( "field", "field" );
			assertThat( query.uniqueResult() ).isNotNull();
		} );

	}

	@AfterAll
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}
}
