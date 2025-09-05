/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.dynamic;

import org.hibernate.query.restriction.Path;
import org.hibernate.query.specification.ProjectionSpecification;
import org.hibernate.query.specification.SelectionSpecification;
import org.hibernate.query.specification.SimpleProjectionSpecification;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {BasicEntity.class, OtherEntity.class})
@org.hibernate.testing.orm.junit.SessionFactory(useCollectingStatementInspector = true)
public class ProjectionSpecificationTest {

	@BeforeAll
	public static void setup(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.createMutationQuery( "insert BasicEntity (id, name, position) values (1, 'Gavin', 2)" )
					.executeUpdate();
		} );
	}

	@Test
	void testProjection(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var spec = SelectionSpecification.create( BasicEntity.class );
			var projection = ProjectionSpecification.create( spec );
			var position = projection.select( BasicEntity_.position );
			var name = projection.select( BasicEntity_.name );
			var id = projection.select( Path.from( BasicEntity.class ).to( BasicEntity_.id ) );
			var otherId = projection.select( Path.from( BasicEntity.class ).to( BasicEntity_.other ).to( OtherEntity_.id ) );
			var tuple = projection.createQuery( session ).getSingleResult();
			assertEquals( 2, position.in(tuple) );
			assertEquals( "Gavin", name.in(tuple) );
			assertEquals( 1, id.in(tuple) );
			assertNull( otherId.in( tuple ) );
		});
	}

	@Test
	void testSimpleProjection(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var spec = SelectionSpecification.create( BasicEntity.class );
			var projection = SimpleProjectionSpecification.create( spec, BasicEntity_.name );
			var name = projection.createQuery( session ).getSingleResult();
			assertEquals( "Gavin", name );
		});
	}

	@Test
	void testSimpleProjectionPath(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var spec = SelectionSpecification.create( BasicEntity.class );
			var projection = SimpleProjectionSpecification.create( spec,
					Path.from( BasicEntity.class)
							.to( BasicEntity_.other )
							.to( OtherEntity_.id ) );
			var id = projection.createQuery( session ).getSingleResult();
			assertNull( id );
		});
	}
}
