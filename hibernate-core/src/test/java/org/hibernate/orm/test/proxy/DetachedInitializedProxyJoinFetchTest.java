package org.hibernate.orm.test.proxy;

import java.util.List;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A query that join fetches a lazy to-one of a managed owner must not fail
 * when the owner still references an initialized proxy that the session no
 * longer manages, for example because the target was detached.
 */
@DomainModel(
		annotatedClasses = {
				DetachedInitializedProxyJoinFetchTest.Owner.class,
				DetachedInitializedProxyJoinFetchTest.Target.class
		}
)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-21019")
public class DetachedInitializedProxyJoinFetchTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Target target = new Target();
			target.id = 1L;
			target.name = "target";
			session.persist( target );

			Owner owner = new Owner();
			owner.id = 1L;
			owner.target = target;
			session.persist( owner );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testJoinFetchOfDetachedInitializedProxy(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Owner owner = session.find( Owner.class, 1L );
			Hibernate.initialize( owner.target );
			session.detach( owner.target );

			List<Owner> owners = session.createSelectionQuery(
					"select o from Owner o left join fetch o.target",
					Owner.class
			).getResultList();

			assertThat( owners ).containsExactly( owner );
			assertThat( Hibernate.isInitialized( owner.target ) ).isTrue();
			assertThat( Hibernate.unproxy( owner.target, Target.class ).name ).isEqualTo( "target" );
		} );
	}

	@Test
	public void testJoinFetchOfDetachedUninitializedProxy(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Owner owner = session.find( Owner.class, 1L );
			session.detach( owner.target );

			List<Owner> owners = session.createSelectionQuery(
					"select o from Owner o left join fetch o.target",
					Owner.class
			).getResultList();

			assertThat( owners ).containsExactly( owner );
		} );
	}

	@Entity(name = "Owner")
	@Table(name = "owner_tbl")
	public static class Owner {
		@Id
		Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		Target target;
	}

	@Entity(name = "Target")
	@Table(name = "target_tbl")
	public static class Target {
		@Id
		Long id;

		String name;
	}
}
