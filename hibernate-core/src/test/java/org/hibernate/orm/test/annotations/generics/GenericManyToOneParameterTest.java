/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.annotations.generics;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		GenericManyToOneParameterTest.SiteImpl.class,
		GenericManyToOneParameterTest.UserSiteImpl.class,
		GenericManyToOneParameterTest.BarImpl.class,
		GenericManyToOneParameterTest.BarBarImpl.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17402" )
public class GenericManyToOneParameterTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final UserSiteImpl userSite = new UserSiteImpl();
			userSite.setId( 1L );
			userSite.setName( "user_site_1" );
			session.persist( userSite );
			final BarImpl bar = new BarImpl();
			bar.setId( 1L );
			bar.setSite( userSite );
			session.persist( bar );
		} );
	}

	@AfterAll
	public void tearDown(final SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from SiteImpl" );
			session.createMutationQuery( "delete from BarImpl" );
		} );
	}

	@Test
	public void testParamComparison(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final UserSiteImpl site = session.find( UserSiteImpl.class, 1L );
			final BarImpl result = session.createQuery(
					"select b from BarImpl b join b.site s where s = ?1",
					BarImpl.class
			).setParameter( 1, site ).getSingleResult();
			assertThat( result.getSite() ).isNotNull();
			assertThat( result.getSite() ).isInstanceOf( SiteImpl.class );
			assertThat( result.getSite().getId() ).isEqualTo( site.getId() );
			assertThat( result.getSite().getName() ).isEqualTo( site.getName() );
		} );
	}

	@Test
	public void testParamInList(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final UserSiteImpl site = session.find( UserSiteImpl.class, 1L );
			final BarImpl result = session.createQuery(
					"select b from BarImpl b join b.site s where s in (?1)",
					BarImpl.class
			).setParameter( 1, site ).getSingleResult();
			assertThat( result.getSite() ).isNotNull();
			assertThat( result.getSite() ).isInstanceOf( SiteImpl.class );
			assertThat( result.getSite().getId() ).isEqualTo( site.getId() );
			assertThat( result.getSite().getName() ).isEqualTo( site.getName() );
		} );
	}

	public interface EntityWithId {
		Long getId();
	}

	public interface Site extends EntityWithId {
	}

	public interface UserSite extends Site {
		String getName();
	}

	@Entity( name = "SiteImpl" )
	public static abstract class SiteImpl implements Site {
		@Id
		protected Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity( name = "UserSiteImpl" )
	public static class UserSiteImpl extends SiteImpl implements UserSite {
	}

	public interface Bar extends EntityWithId {
	}

	public interface SitedBar<S extends Site> extends Bar {
		S getSite();
	}

	@MappedSuperclass
	public static abstract class BarBarImpl<T extends Site> implements SitedBar<T> {
		@ManyToOne( fetch = FetchType.LAZY, targetEntity = SiteImpl.class )
		@JoinColumn( name = "BAR_ID" )
		private T site;

		@Override
		public T getSite() {
			return (T) site;
		}

		public void setSite(final T site) {
			this.site = site;
		}

	}

	@Entity( name = "BarImpl" )
	public static class BarImpl extends BarBarImpl<UserSite> implements Bar {
		@Id
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}
	}
}
