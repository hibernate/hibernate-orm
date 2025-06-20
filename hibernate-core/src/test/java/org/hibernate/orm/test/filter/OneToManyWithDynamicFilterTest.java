/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ParamDef;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				OneToManyWithDynamicFilterTest.ArticleRevision.class,
				OneToManyWithDynamicFilterTest.ArticleTrading.class
		}
)
@SessionFactory
public class OneToManyWithDynamicFilterTest extends AbstractStatefulStatelessFilterTest {

	@BeforeEach
	void setUp() {
		scope.inTransaction( session -> {
			final ArticleTrading articleTrading = new ArticleTrading();
			articleTrading.setClassifier( "no_classification" );
			articleTrading.setPartyId( 2 );
			articleTrading.setDeletionTimestamp( Timestamp.valueOf( "9999-12-31 00:00:00" ) );
			articleTrading.setDeleted( true );

			final ArticleRevision revision = new ArticleRevision();
			revision.addArticleTradings( articleTrading );
			revision.setDeletionTimestamp( Timestamp.valueOf( "9999-12-31 00:00:00" ) );
			revision.setDeleted( true );
			session.persist( revision );
		} );
	}

	@AfterEach
	void tearDown() {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	void testForIssue(BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
		inTransaction.accept(scope, session -> {
			final org.hibernate.Filter enableFilter = session.enableFilter( "aliveOnly" );
			enableFilter.setParameter( "aliveTimestamp", Timestamp.valueOf( "9999-12-31 00:00:00" ) );
			enableFilter.setParameter( "deleted", true );
			enableFilter.validate();

			final Query<Long> query = session.createQuery( "select a.id from ArticleRevision as a " +
															"left join a.articleTradings as t " +
															"with ( (t.partyId = :p_0)  and  (t.classifier = :p_1) )", Long.class );
			query.setParameter( "p_0", 1L );
			query.setParameter( "p_1", "no_classification" );
			final List<Long> list = query.getResultList();
			assertThat( list.size(), is( 1 ) );

		} );
	}

	@Entity(name = "ArticleRevision")
	@Table(name = "REVISION")
	@FilterDefs({
			@FilterDef(
					name = "aliveOnly",
					parameters = {
						@ParamDef(name = "aliveTimestamp", type = Timestamp.class),
						@ParamDef(name = "deleted", type = Boolean.class)
					},
					defaultCondition = "DELETION_TIMESTAMP = :aliveTimestamp and DELETED = :deleted")
	})
	@Filters( { @Filter(name = "aliveOnly", condition = "DELETION_TIMESTAMP = :aliveTimestamp and DELETED = :deleted") } )
	public static class ArticleRevision {
		@Id
		@GeneratedValue
		private long id;

		@Column(name = "DELETION_TIMESTAMP")
		private Timestamp deletionTimestamp;

		@Column(name = "DELETED")
		private boolean deleted;

		@OneToMany(mappedBy = "articleRevision", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		@Filter(name = "aliveOnly")
		private Set<ArticleTrading> articleTradings = new HashSet<>();

		public void setDeletionTimestamp(Timestamp deletionTimestamp) {
			this.deletionTimestamp = deletionTimestamp;
		}


		public void setDeleted(boolean deleted) {
			this.deleted = deleted;
		}

		public void addArticleTradings(ArticleTrading articleTrading) {
			this.articleTradings.add( articleTrading );
			articleTrading.setArticleRevision( this );
		}
	}

	@Entity(name = "ArticleTrading")
	@Table(name = "TRADING")
	public static class ArticleTrading {
		@Id
		@GeneratedValue
		private long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "articleRevision", nullable = false)
		private ArticleRevision articleRevision;

		private long partyId;

		private String classifier;

		@Column(name = "DELETED")
		private boolean deleted;

		@Column(name = "DELETION_TIMESTAMP")
		protected Timestamp deletionTimestamp;

		public void setArticleRevision(ArticleRevision articleRevision) {
			this.articleRevision = articleRevision;
		}

		public void setPartyId(long partyId) {
			this.partyId = partyId;
		}

		public void setClassifier(String classifier) {
			this.classifier = classifier;
		}

		public void setDeletionTimestamp(Timestamp deletionTimestamp) {
			this.deletionTimestamp = deletionTimestamp;
		}

		public void setDeleted(boolean deleted) {
			this.deleted = deleted;
		}
	}
}
