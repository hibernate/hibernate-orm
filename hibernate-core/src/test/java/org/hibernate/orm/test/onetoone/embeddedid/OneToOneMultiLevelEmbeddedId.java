/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.embeddedid;

import java.util.List;

import org.hibernate.query.Query;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaQuery;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@DomainModel(
		annotatedClasses = {
				OneToOneMultiLevelEmbeddedId.Primary.class,
				OneToOneMultiLevelEmbeddedId.Secondary.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-15279")
public class OneToOneMultiLevelEmbeddedId {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final TopId id = new TopId( 1, 2 );
					final Secondary secondary = new Secondary( id );
					final Primary primary = new Primary( secondary );

					session.persist( secondary );
					session.persist( primary );
				}
		);
	}

	@Test
	public void testIt(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();

					final CriteriaQuery<Primary> query = criteriaBuilder.createQuery( Primary.class );

					query.select( query.from( Primary.class ) );

					final Query<Primary> typedQuery = session.createQuery( query );
					final List<Primary> results = typedQuery.getResultList();
					assertThat( results.size(), is( 1 ) );
				}
		);
	}

	@Embeddable
	public static class NestedId {

		private int nid;

		public NestedId() {
		}

		public NestedId(int nid) {
			this.nid = nid;
		}
	}

	@Embeddable
	public static class TopId {

		private int tid;

		@Embedded
		private NestedId nestedId;

		public TopId() {
		}

		public TopId(int tid, int nid) {
			this.tid = tid;
			this.nestedId = new NestedId( nid );
		}
	}

	@Entity(name = "Secondary")
	@Table(name = "secondary_table")
	public static class Secondary {

		@EmbeddedId
		private TopId id;

		public Secondary() {
		}

		public Secondary(TopId id) {
			this.id = id;
		}

		public TopId getId() {
			return id;
		}
	}

	@Entity(name = "Primary")
	@Table(name = "primary_table")
	public static class Primary {

		@Id
		@OneToOne
		@JoinColumn(name = "ptid", referencedColumnName = "tid")
		@JoinColumn(name = "pnid", referencedColumnName = "nid")
		private Secondary secondary;

		public Primary() {
		}

		public Primary(Secondary secondary) {
			this.secondary = secondary;
		}

		public Secondary getSecondary() {
			return secondary;
		}
	}
}
