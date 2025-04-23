/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.optimizer;

import org.hibernate.orm.test.bytecode.enhancement.optimizer.child.ChildEntity;
import org.hibernate.query.Query;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.Jira;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@Jira("https://hibernate.atlassian.net/browse/HHH-19372")
@RunWith( BytecodeEnhancerRunner.class )
public class HierarchyBytecodeOptimizerTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				ParentEntity.class,
				ChildEntity.class,
		};
	}

	@Test
	public void testOptimizerSetPropertyValues() {
		ChildEntity childEntity = new ChildEntity();
		childEntity.setId( 1L );
		childEntity.setField( "field" );
		childEntity.setChieldField( "childField" );

		inTransaction( session -> {
			session.persist( childEntity );
		} );

		inTransaction( session -> {
			Query<ChildEntity> query = session.createQuery( "select c from ChildEntity c where c.field = :field",
					ChildEntity.class );
			query.setParameter( "field", "field" );
			assertThat( query.uniqueResult() ).isNotNull();
		} );

	}

	@After
	public void cleanup() {
		sessionFactory().getSchemaManager().truncateMappedObjects();
	}
}
