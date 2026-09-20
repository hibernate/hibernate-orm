/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = { ToOneAssociationVisibilityTest.Person.class,
		ToOneAssociationVisibilityTest.Owner.class, ToOneAssociationVisibilityTest.Profile.class,
		ToOneAssociationVisibilityTest.Account.class })
@SessionFactory(generateStatistics = true, useCollectingStatementObserver = true)
@ServiceRegistry(settings = {
		@Setting(name = "hibernate.cache.use_second_level_cache", value = "true"),
		@Setting(name = "hibernate.cache.use_query_cache", value = "true")
})
class ToOneAssociationVisibilityTest {
	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 2; id++ ) {
				final Person person = new Person();
				person.id = id;
				person.active = id == 1 ? 1 : 0;
				person.category = 1;
				person.allowed = id == 1 ? 1 : 0;
				session.persist( person );
				final Owner owner = new Owner();
				owner.id = id;
				owner.sql = person;
				owner.filtered = person;
				owner.unrestricted = person;
				owner.combined = person;
				owner.details = person;
				session.persist( owner );
				final Profile profile = new Profile();
				profile.id = id;
				session.persist( profile );
				final Account account = new Account();
				account.id = id;
				account.active = person.active;
				account.profile = profile;
				account.linkedProfile = profile;
				session.persist( account );
			}
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.getSessionFactory().getCache().evictAllRegions();
	}

	@ParameterizedTest
	@ValueSource(strings = { "find", "fetch", "native", "cache" })
	void visibilityBelongsToTheAssociation(String loading, SessionFactoryScope scope) {
		for ( boolean preload : new boolean[] { false, true } ) {
			scope.inTransaction( session -> {
				session.enableFilter( "roleActive" ).setParameter( "active", 1 );
				session.enableFilter( "roleCategory" ).setParameter( "category", 1 );
				session.enableFilter( "roleDetails" ).setParameter( "allowed", 1 );
				final Person person = preload ? session.find( Person.class, 2L ) : null;
				final Owner owner = switch ( loading ) {
					case "find" -> session.find( Owner.class, 2L );
					case "native" -> session.createNativeQuery( "select * from role_owner where id=2", Owner.class )
							.getSingleResult();
					default -> session.createQuery( "from Owner o left join fetch o.sql left join fetch o.filtered "
							+ "left join fetch o.unrestricted left join fetch o.combined left join fetch o.details where o.id=2",
							Owner.class ).setCacheable( loading.equals( "cache" ) ).getSingleResult();
				};
				assertThat( owner.sql ).isNull();
				assertThat( owner.filtered ).isNull();
				assertThat( owner.details ).isNull();
				assertThat( owner.combined ).isNull();
				assertThat( owner.unrestricted ).isNotNull();
				assertThat( session.find( Person.class, 2L ) ).isSameAs( owner.unrestricted );
				if ( preload ) {
					assertThat( owner.unrestricted ).isSameAs( person );
				}
				owner.name = "changed";
			} );
		}
		scope.inTransaction( session -> assertThat( session.createNativeQuery(
				"select sql_id, filtered_id, details_id, combined_id from role_owner where id=2", Object[].class )
				.getSingleResult() ).containsExactly( 2L, 2L, 2L, 2L ) );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void enabledFiltersAreCombinedAndParametersAreNotCached(boolean nativeQuery, SessionFactoryScope scope) {
		for ( int category : new int[] { 1, 2, 1 } ) {
			scope.inTransaction( session -> {
				session.enableFilter( "roleActive" ).setParameter( "active", 1 );
				session.enableFilter( "roleCategory" ).setParameter( "category", category );
				final Owner owner = nativeQuery
						? session.createNativeQuery( "select * from role_owner where id=1", Owner.class ).getSingleResult()
						: session.find( Owner.class, 1L );
				assertThat( owner.filtered == null ).isEqualTo( category != 1 );
				assertThat( owner.combined == null ).isEqualTo( category != 1 );
				assertThat( owner.sql ).isSameAs( session.find( Person.class, 1L ) );
			} );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void identifierNavigationHonorsAssociationFilters(boolean enabled, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			if ( enabled ) {
				session.enableFilter( "roleActive" ).setParameter( "active", 1 );
			}
			assertThat( session.createQuery( "select o.id from Owner o where o.filtered.id=2", Long.class ).getResultList() )
					.containsExactly( enabled ? new Long[0] : new Long[] { 2L } );
			assertThat( session.createQuery( "select o.id from Owner o where o.sql.id=2", Long.class ).getResultList() ).isEmpty();
			assertThat( session.createQuery( "select o.id from Owner o where fk(o.sql)=2", Long.class ).getResultList() )
					.containsExactly( 2L );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = { "find", "fetch", "native", "circular" })
	void inverseOneToOneRestrictionsAreIndependent(String loading, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "roleActive" ).setParameter( "active", 1 );
			final var account = session.find( Account.class, 2L );
			final Profile loaded = switch ( loading ) {
				case "find" -> session.find( Profile.class, 2L );
				case "circular" -> session.createQuery( "from Account a join fetch a.profile where a.id=2", Account.class )
						.getSingleResult().profile;
				case "native" -> session.createNativeQuery( "select p.*, j.account_id from role_profile p "
						+ "left join role_account_profile j on j.profile_id=p.id where p.id=2", Profile.class ).getSingleResult();
				default -> session.createQuery( "from Profile p left join fetch p.sql left join fetch p.filtered "
						+ "left join fetch p.unrestricted where p.id=2", Profile.class ).getSingleResult();
			};
			final Profile profile = Hibernate.unproxy( loaded, Profile.class );
			assertThat( profile.sql ).isNull();
			assertThat( profile.filtered ).isNull();
			assertThat( profile.unrestricted ).isSameAs( account );
			assertThat( profile.sqlJoin ).isNull();
			assertThat( profile.filteredJoin ).isNull();
			assertThat( profile.unrestrictedJoin ).isSameAs( account );
		} );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void collectionBackReferencesStillCheckVisibility(boolean fetch, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "roleActive" ).setParameter( "active", 1 );
			for ( long id = 1; id <= 2; id++ ) {
				final Person person = fetch
						? session.createQuery( "from Person p left join fetch p.sqlOwners left join fetch p.filteredOwners where p.id=:id", Person.class )
								.setParameter( "id", id ).getSingleResult()
						: session.find( Person.class, id );
				assertThat( person.sqlOwners ).hasSize( 1 );
				assertThat( person.filteredOwners ).hasSize( 1 );
				final Owner owner = person.sqlOwners.iterator().next();
				assertThat( person.filteredOwners.iterator().next() ).isSameAs( owner );
				assertThat( owner.sql ).isSameAs( id == 1 ? person : null );
				assertThat( owner.filtered ).isSameAs( id == 1 ? person : null );
				owner.name = "changed";
			}
		} );
		scope.inTransaction( session -> assertThat( session.createNativeQuery(
				"select sql_id, filtered_id from role_owner where id=2", Object[].class ).getSingleResult() )
				.containsExactly( 2L, 2L ) );
	}

	@Entity(name = "Person")
	@Table(name = "role_person")
	@SecondaryTable(name = "role_person_details")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@FilterDef(name = "roleActive", defaultCondition = "active = :active", parameters = @ParamDef(name = "active", type = Integer.class))
	@FilterDef(name = "roleCategory", defaultCondition = "category = :category", parameters = @ParamDef(name = "category", type = Integer.class))
	@FilterDef(name = "roleDetails", parameters = @ParamDef(name = "allowed", type = Integer.class))
	static class Person {
		@Id Long id;
		int active;
		int category;
		@Column(table = "role_person_details")
		Integer allowed;
		@OneToMany(mappedBy = "sql")
		Set<Owner> sqlOwners;
		@OneToMany(mappedBy = "filtered")
		Set<Owner> filteredOwners;
	}

	@Entity(name = "Owner")
	@Table(name = "role_owner")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Owner {
		@Id Long id;
		String name;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "sql_id")
		@SQLRestriction("active = 1")
		Person sql;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "filtered_id")
		@Filter(name = "roleActive")
		@Filter(name = "roleCategory")
		Person filtered;
		@ManyToOne(fetch = FetchType.LAZY)
		Person unrestricted;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "combined_id")
		@SQLRestriction("active = 1")
		@Filter(name = "roleCategory")
		Person combined;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "details_id")
		@Filter(name = "roleDetails", condition = "{d}.allowed = :allowed", deduceAliasInjectionPoints = false,
				aliases = @SqlFragmentAlias(alias = "d", table = "role_person_details"))
		Person details;
	}

	@Entity(name = "Profile")
	@Table(name = "role_profile")
	static class Profile {
		@Id Long id;
		@OneToOne(mappedBy = "profile")
		@SQLRestriction("active = 1")
		Account sql;
		@OneToOne(mappedBy = "profile")
		@Filter(name = "roleActive")
		Account filtered;
		@OneToOne(mappedBy = "profile")
		Account unrestricted;
		@OneToOne(mappedBy = "linkedProfile")
		@SQLRestriction("active = 1")
		Account sqlJoin;
		@OneToOne(mappedBy = "linkedProfile")
		@Filter(name = "roleActive")
		Account filteredJoin;
		@OneToOne(mappedBy = "linkedProfile")
		Account unrestrictedJoin;
	}

	@Entity(name = "Account")
	@Table(name = "role_account")
	static class Account {
		@Id Long id;
		int active;
		@OneToOne(fetch = FetchType.LAZY)
		Profile profile;
		@OneToOne(fetch = FetchType.LAZY)
		@JoinTable(name = "role_account_profile", joinColumns = @JoinColumn(name = "account_id"),
				inverseJoinColumns = @JoinColumn(name = "profile_id"))
		Profile linkedProfile;
	}
}
