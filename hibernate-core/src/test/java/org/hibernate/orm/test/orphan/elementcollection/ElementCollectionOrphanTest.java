/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.orphan.elementcollection;

import java.util.Collections;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@JiraKey(value = "HHH-14597")
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/orphan/elementcollection/student.hbm.xml"
)
@SessionFactory
public class ElementCollectionOrphanTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void setCompositeElementTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EnrollableClass aClass = new EnrollableClass();
					aClass.setId( "123" );
					aClass.setName( "Math" );
					session.persist( aClass );

					Student aStudent = new Student();
					aStudent.setId( "s1" );
					aStudent.setFirstName( "John" );
					aStudent.setLastName( "Smith" );

					EnrolledClassSeat seat = new EnrolledClassSeat();
					seat.setId( "seat1" );
					seat.setRow( 10 );
					seat.setColumn( 5 );

					StudentEnrolledClass enrClass = new StudentEnrolledClass();
					enrClass.setEnrolledClass( aClass );
					enrClass.setClassStartTime( 130 );
					enrClass.setSeat( seat );
					aStudent.setEnrolledClasses( Collections.singleton( enrClass ) );
					session.persist( aStudent );
				}
		);
	}
}
