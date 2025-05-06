/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sqm.exec;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.junit.jupiter.api.Test;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

/**
 * Test that uses a subquery that makes an implicit join to a previously resolved implicit join in the
 * outer query.  This test verifies that the query succeeds when such a query is executed.
 *
 * @author Chris Cranford
 */
@DomainModel(annotatedClasses = {
		SubQueryImplicitJoinReferenceTest.TheEntity.class,
		SubQueryImplicitJoinReferenceTest.RevisionInfoEntity.class })
@SessionFactory
@JiraKey(value = "HHH-14482")
public class SubQueryImplicitJoinReferenceTest {

	@Test
	public void performDataPreparation(SessionFactoryScope scope) {
		// Simulate creating revision 1
		final RevisionInfoEntity revEntity = new RevisionInfoEntity();
		revEntity.setId( 1 );

		// Simulate creating the audit record
		final TheEntity entity = new TheEntity();
		entity.setOriginalId( OriginalId.from( revEntity, 1 ) );
		entity.setData( "Test" );

		// Persist the entities
		scope.inTransaction( session -> { session.persist( revEntity ); session.persist( entity ); } );
	}

	@Test
	public void performHqlTest(SessionFactoryScope scope) {
		// Now simulate running an audit query
		scope.inSession( session -> {
			session.createQuery( "select e__ FROM TheEntity e__ "
										+ "WHERE e__.originalId.rev.id = (select max(e2__.originalId.rev.id) FROM "
										+ "TheEntity e2__ WHERE " +
										"e2__.originalId.rev.id <= 2 and e__.originalId.id = e2__.originalId.id)" ).list();
		} );
	}

	@Test
	public void performHqlIsNullTest(SessionFactoryScope scope) {
		// Now simulate running an audit query
		scope.inSession( session -> {
			session.createQuery( "select e__ FROM TheEntity e__ "
										+ "WHERE e__.originalId.rev.id = (select max(e2__.originalId.rev.id) FROM "
										+ "TheEntity e2__ WHERE " +
										"e2__.originalId.rev is null)" ).list();
		} );
	}

	@Test
	public void performHqlTest2(SessionFactoryScope scope) {
		// Now simulate running an audit query
		scope.inSession( session -> {
			session.createQuery( "select e__ FROM TheEntity e__ "
										+ "WHERE e__.originalId.id = (select max(e2__.originalId.id) FROM "
										+ "TheEntity e2__ WHERE " +
										"e__.originalId.id = e2__.originalId.id and e2__.originalId.rev.id <= 2)" ).list();
		} );
	}

	@Test
	public void performHqlTest3(SessionFactoryScope scope) {
		// Now simulate running an audit query
		scope.inSession( session -> {
			session.createQuery( "select e2__.originalId.id, e2__.originalId.rev.id  FROM "
										+ "TheEntity e2__ WHERE " +
										" e2__.originalId.rev.id <= 2" ).list();
		} );
	}

	@Entity(name = "TheEntity")
	public static class TheEntity {
		@EmbeddedId
		private OriginalId originalId;
		private String data;

		public OriginalId getOriginalId() {
			return originalId;
		}

		public void setOriginalId(OriginalId originalId) {
			this.originalId = originalId;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}
	}

	@Entity(name="RevisionInfoEntity")
	public static class RevisionInfoEntity {
		@Id
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Embeddable
	public static class OriginalId implements Serializable {
		@ManyToOne
		private RevisionInfoEntity rev;
		private Integer id;

		public RevisionInfoEntity getRev() {
			return rev;
		}

		public void setRev(RevisionInfoEntity rev) {
			this.rev = rev;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public static OriginalId from(RevisionInfoEntity rev, Integer id) {
			OriginalId oId = new OriginalId();
			oId.rev = rev;
			oId.id = id;
			return oId;
		}

		@Override
		public int hashCode() {
			return Objects.hash(rev, id);
		}

		@Override
		public boolean equals(Object obj) {
			if ( obj == null ) {
				return false;
			}
			if ( obj.getClass() != this.getClass() ) {
				return false;
			}

			final OriginalId other = (OriginalId) obj;
			return Objects.equals( rev, other.rev ) && Objects.equals( id, other.id );
		}
	}
}
