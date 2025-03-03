/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.uniquekey;

import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.annotations.NaturalId;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@JiraKey(value = "HHH-15784")
@DomainModel(annotatedClasses = PrimitiveArrayNaturalIdTest.HashedContent.class)
@SessionFactory
public class PrimitiveArrayNaturalIdTest {

	@Test
	public void testPersistByteArrayNaturalId(SessionFactoryScope scope) {
		final byte[] naturalId = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
		scope.inTransaction(
				session -> {
					HashedContent content = new HashedContent(
							1,
							naturalId,
							"content"
					);

					session.persist( content );
				}
		);

		scope.inTransaction(
				session -> {
					NaturalIdLoadAccess<HashedContent> content = session.byNaturalId( HashedContent.class ).using(
							"binaryHash",
							naturalId
					);
					assertNotNull(content);
				}
		);
	}

	@Entity(name = "HashedContent")
	public static class HashedContent {
		@Id
		private Integer id;

		@NaturalId
		private byte[] binaryHash;

		private String name;

		public HashedContent() {
		}

		public HashedContent(Integer id, byte[] binaryHash, String name) {
			this.id = id;
			this.binaryHash = binaryHash;
			this.name = name;
		}
	}

}
