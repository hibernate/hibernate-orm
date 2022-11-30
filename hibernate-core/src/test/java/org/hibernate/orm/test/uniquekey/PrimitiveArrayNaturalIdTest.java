package org.hibernate.orm.test.uniquekey;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.NaturalId;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@TestForIssue(jiraKey = "HHH-15784")
public class PrimitiveArrayNaturalIdTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				HashedContent.class
		};
	}

	@Test
	public void testPersistByteArrayNaturalId() {
		try (Session s = openSession()) {
			Transaction tx = s.beginTransaction();

			HashedContent content = new HashedContent();
			content.id = 1;
			content.binaryHash = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };

			s.persist( content );

			tx.commit();
		}
	}

	@Entity(name = "HashedContent")
	public static class HashedContent {
		@Id
		private Integer id;

		@NaturalId
		private byte[] binaryHash;
	}

}
