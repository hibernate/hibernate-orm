/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test using an entity which is in no package.
 * We had problems with ByteBuddy in the past.
 */
@JiraKey(value = "HHH-13112")
@DomainModel(annotatedClasses = {AnnotationMappedNoPackageEntity.class})
@SessionFactory
public class NoPackageTest {

	@Test
	public void testNoException(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			AnnotationMappedNoPackageEntity box = new AnnotationMappedNoPackageEntity();
			box.setId( 42 );
			box.setName( "This feels dirty" );
			session.persist( box );
		} );

		scope.inTransaction( session -> {
			Query<AnnotationMappedNoPackageEntity> query = session.createQuery(
					"select e from " + AnnotationMappedNoPackageEntity.class.getSimpleName() + " e",
					AnnotationMappedNoPackageEntity.class
			);
			AnnotationMappedNoPackageEntity box = query.getSingleResult();
			assertEquals( (Integer) 42, box.getId() );
		} );
	}
}
