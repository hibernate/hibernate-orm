package org.hibernate.orm.test.query.hql;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		NestedOneToOneFetchWithoutAliasTest.Responsible.class,
		NestedOneToOneFetchWithoutAliasTest.Account.class,
		NestedOneToOneFetchWithoutAliasTest.AccountIndex.class,
		NestedOneToOneFetchWithoutAliasTest.Detail.class
})
@SessionFactory(useCollectingStatementObserver = true)
@Jira("https://hibernate.atlassian.net/browse/HHH-15215")
public class NestedOneToOneFetchWithoutAliasTest {
	private static final String QUERY = """
			from FetchResponsible r
			left join fetch r.responsibleDetail
			left join fetch r.account
			left join fetch r.account.accountDetail
			left join fetch r.account.accountIndex
			left join fetch r.account.accountIndex.accountAggregate
			where r.userLogon = :logon
			""";

	@BeforeAll
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var responsible = new Responsible();
			responsible.id = 1L;
			responsible.userLogon = "populated";
			responsible.responsibleDetail = new Detail( 10L, "responsible detail" );
			responsible.account = new Account();
			responsible.account.id = 2L;
			responsible.account.accountDetail = new Detail( 11L, "account detail" );
			responsible.account.accountIndex = new AccountIndex();
			responsible.account.accountIndex.id = 3L;
			responsible.account.accountIndex.accountAggregate = new Detail( 12L, "aggregate" );
			session.persist( responsible );
			final var empty = new Responsible();
			empty.id = 4L;
			empty.userLogon = "empty";
			session.persist( empty );
		} );
	}

	@Test
	void testNestedFetchesWithoutAliases(SessionFactoryScope scope) {
		final var observer = scope.getCollectingStatementObserver();
		scope.inTransaction( session -> {
			observer.clear();
			final var result = session.createQuery( QUERY, Responsible.class )
					.setParameter( "logon", "populated" ).getSingleResult();
			assertThat( result.id ).isEqualTo( 1L );
			assertThat( Hibernate.isInitialized( result.responsibleDetail ) ).isTrue();
			assertThat( Hibernate.isInitialized( result.account ) ).isTrue();
			assertThat( Hibernate.isInitialized( result.account.accountDetail ) ).isTrue();
			assertThat( Hibernate.isInitialized( result.account.accountIndex ) ).isTrue();
			assertThat( Hibernate.isInitialized( result.account.accountIndex.accountAggregate ) ).isTrue();
			assertThat( result.responsibleDetail.value ).isEqualTo( "responsible detail" );
			assertThat( result.account.id ).isEqualTo( 2L );
			assertThat( result.account.accountDetail.value ).isEqualTo( "account detail" );
			assertThat( result.account.accountIndex.id ).isEqualTo( 3L );
			assertThat( result.account.accountIndex.accountAggregate.value ).isEqualTo( "aggregate" );
			assertThat( observer.getSqlQueries() ).hasSize( 1 );
		} );
		scope.inTransaction( session -> {
			observer.clear();
			final var result = session.createQuery( QUERY, Responsible.class )
					.setParameter( "logon", "empty" ).getSingleResult();
			assertThat( result.id ).isEqualTo( 4L );
			assertThat( result.responsibleDetail ).isNull();
			assertThat( result.account ).isNull();
			assertThat( observer.getSqlQueries() ).hasSize( 1 );
		} );
	}

	@AfterAll
	void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from FetchResponsible" ).executeUpdate();
			session.createMutationQuery( "delete from FetchAccount" ).executeUpdate();
			session.createMutationQuery( "delete from FetchAccountIndex" ).executeUpdate();
			session.createMutationQuery( "delete from FetchDetail" ).executeUpdate();
		} );
	}

	@Entity(name = "FetchResponsible")
	@Table(name = "hhh15215_responsible")
	public static class Responsible {
		@Id
		Long id;
		String userLogon;
		@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		Detail responsibleDetail;
		@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		Account account;
	}

	@Entity(name = "FetchAccount")
	@Table(name = "hhh15215_account")
	public static class Account {
		@Id
		Long id;
		@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		Detail accountDetail;
		@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		AccountIndex accountIndex;
	}

	@Entity(name = "FetchAccountIndex")
	@Table(name = "hhh15215_account_index")
	public static class AccountIndex {
		@Id
		Long id;
		@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		Detail accountAggregate;
	}

	@Entity(name = "FetchDetail")
	@Table(name = "hhh15215_detail")
	public static class Detail {
		@Id
		Long id;
		@Column(name = "detail_value")
		String value;

		public Detail() {
		}

		Detail(Long id, String value) {
			this.id = id;
			this.value = value;
		}
	}
}
