/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.dynamic;

import org.hibernate.query.restriction.Path;
import org.hibernate.query.specification.SelectionSpecification;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
			session.createMutationQuery( "insert OtherEntity (id) values (69)" )
					.executeUpdate();
			session.createMutationQuery( "insert BasicEntity (id, name, position, other) values (1, 'Gavin', 2, (select o from OtherEntity o where o.id = 69))" )
					.executeUpdate();
		} );
	}

	@Test
	void testProjection(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var spec = SelectionSpecification.create( BasicEntity.class );
			var projection = spec.createProjection();
			var position = projection.select( BasicEntity_.position );
			var name = projection.select( BasicEntity_.name );
			var id = projection.select( Path.from( BasicEntity.class ).to( BasicEntity_.id ) );
			var otherId = projection.select( Path.from( BasicEntity.class ).to( BasicEntity_.other ).to( OtherEntity_.id ) );
			var tuple = projection.createQuery( session ).getSingleResult();
			assertEquals( 2, position.in(tuple) );
			assertEquals( "Gavin", name.in(tuple) );
			assertEquals( 1, id.in(tuple) );
			assertEquals( 69, otherId.in( tuple ) );
		});
	}

	@Test
	void testSimpleProjection(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var spec = SelectionSpecification.create( BasicEntity.class );
			var projection = spec.createProjection( BasicEntity_.name );
			var name = projection.createQuery( session ).getSingleResult();
			assertEquals( "Gavin", name );
		});
	}

	@Test
	void testSimpleProjectionPath(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var spec = SelectionSpecification.create( BasicEntity.class );
			var projection =
					spec.createProjection(
							Path.from( BasicEntity.class)
									.to( BasicEntity_.other )
									.to( OtherEntity_.id ) );
			var id = projection.createQuery( session ).getSingleResult();
			assertEquals( 69, id );
		});
	}

	@Test
	void testSimpleEntityProjection(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var spec = SelectionSpecification.create( BasicEntity.class );
			var projection = spec.createProjection( BasicEntity_.other );
			var otherEntity = projection.createQuery( session ).getSingleResult();
			assertNotNull( otherEntity );
			assertEquals( 69, otherEntity.id );
		});
	}

	@Test
	void testSimpleEntityProjectionPath(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var spec = SelectionSpecification.create( BasicEntity.class );
			var projection = spec.createProjection( Path.from(BasicEntity.class).to(BasicEntity_.other) );
			var otherEntity = projection.createQuery( session ).getSingleResult();
			assertNotNull( otherEntity );
			assertEquals( 69, otherEntity.id );
		});
	}
}
