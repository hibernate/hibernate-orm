/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.typedescriptor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Lukasz Antoniak
 */
@DomainModel(
		annotatedClasses = VariousTypesEntity.class
)
@SessionFactory
public class ByteTest {
	public static final byte TEST_VALUE = 65;

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					VariousTypesEntity entity = new VariousTypesEntity();
					entity.setId( 1 );
					entity.setByteData( TEST_VALUE );
					session.persist( entity );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-6533")
	public void testByteDataPersistenceAndRetrieval(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					VariousTypesEntity entity = (VariousTypesEntity) session.createQuery(
							" from VariousTypesEntity " +
									" where byteData = org.hibernate.orm.test.typedescriptor.ByteTest.TEST_VALUE "
					).uniqueResult();

					assertNotNull( entity );
					assertEquals( TEST_VALUE, entity.getByteData() );
					entity.setByteData( Byte.MIN_VALUE );
					session.persist( entity );
				}
		);

		// Testing minimal value.
		scope.inTransaction(
				session -> {
					VariousTypesEntity entity = (VariousTypesEntity) session.createQuery(
							" from VariousTypesEntity " +
									" where byteData = java.lang.Byte.MIN_VALUE "
					).uniqueResult();
					assertNotNull( entity );
					assertEquals( Byte.MIN_VALUE, entity.getByteData() );
					entity.setByteData( Byte.MAX_VALUE );
					session.merge( entity );
				}
		);

		// Testing maximal value.
		scope.inTransaction(
				session -> {
					VariousTypesEntity entity = (VariousTypesEntity) session.createQuery(
							" from VariousTypesEntity " +
									" where byteData = java.lang.Byte.MAX_VALUE "
					).uniqueResult();
					assertNotNull( entity );
					assertEquals( Byte.MAX_VALUE, entity.getByteData() );
				}
		);
	}
}
