package org.hibernate.orm.test.aggregation;

import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.SelectionQuery;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

public class UuidAggregationTest extends BaseCoreFunctionalTestCase {

	@Entity
	public static class UuidIdentifiedEntity {

		@Id
		private UUID id;

		public UuidIdentifiedEntity() {
			super();
		}

		public UUID getId() {
			return id;
		}

		public void setId(final UUID id) {
			this.id = id;
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-15495")
	public void hhh15495Test() throws Exception {
		final Session s = openSession();
		final Transaction tx = s.beginTransaction();
		final SelectionQuery<UUID> query = s.createSelectionQuery( "SELECT MAX(id) from UuidIdentifiedEntity;", UUID.class );
		query.getSingleResult();
		tx.commit();
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ UuidIdentifiedEntity.class };
	}
}
