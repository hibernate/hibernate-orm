/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * @author Vlad MIhalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = BinaryTypeTest.Image.class)
@SessionFactory
public class BinaryTypeTest {
	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testByteArrayStringRepresentation(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			Image image = new Image();
			image.id = 1L;
			image.content = new byte[] {1, 2, 3};

			session.persist( image );
		} );

		factoryScope.inTransaction( (session) -> {
			assertArrayEquals( new byte[] {1, 2, 3}, session.find( Image.class, 1L ).content );
		} );
	}

	@Entity(name = "Image")
	public static class Image {

		@Id
		private Long id;

		@Column(name = "content")
		private byte[] content;
	}
}
