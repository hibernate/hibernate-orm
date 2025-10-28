/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.ids.embeddedid;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test avoiding the creating of a foreign key constraint for an embedded identifier that
 * contains a many-to-one relationship, allowing the removal of the base table entity
 * without throwing a foreign-key constraint exception due to historical audit rows.
 *
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-11107")
@EnversTest
@Jpa(annotatedClasses = {CorrectChild.class, IncorrectChild.class, Parent.class})
public class RelationInsideEmbeddableRemoveTest {

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( entityManager -> {
			Parent parent = new Parent( "Parent" );
			parent.addIncorrectChild( 1 );
			parent.addCorrectChild( 1 );
			entityManager.persist( parent );
			for ( IncorrectChild child : parent.getIncorrectChildren() ) {
				entityManager.persist( child );
			}
			for ( CorrectChild child : parent.getCorrectChildren() ) {
				entityManager.persist( child );
			}
		} );

		// Revision 2
		scope.inTransaction( entityManager -> {
			Parent parent = entityManager.find( Parent.class, "Parent" );
			for ( IncorrectChild child : parent.getIncorrectChildren() ) {
				entityManager.remove( child );
			}
			parent.getIncorrectChildren().clear();
			for( CorrectChild child : parent.getCorrectChildren() ) {
				entityManager.remove( child );
			}
			parent.getCorrectChildren().clear();
		} );

		// Revision 3
		// This fails because of referential integrity constraints without fix.
		scope.inTransaction( entityManager -> {
			Parent parent = entityManager.find( Parent.class, "Parent" );
			entityManager.remove( parent );
		} );
	}

	@Test
	public void testRevisionCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( Arrays.asList( 1, 3 ),
					AuditReaderFactory.get( em ).getRevisions( Parent.class, "Parent" ) );
		} );
	}
}
