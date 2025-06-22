/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tenantid;

import org.hibernate.PropertyValueException;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryProducer;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.binder.internal.TenantIdBinder;
import org.hibernate.generator.internal.CurrentTimestampGeneration;
import org.hibernate.orm.test.annotations.MutableClock;
import org.hibernate.orm.test.annotations.MutableClockSettingProvider;
import org.hibernate.query.Query;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

import org.hibernate.testing.orm.junit.SettingProvider;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION;
import static org.hibernate.jpa.HibernateHints.HINT_TENANT_ID;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Map;

@SessionFactory
@DomainModel(annotatedClasses = { Account.class, Client.class, Record.class })
@ServiceRegistry(
		settings = {
				@Setting(name = JAKARTA_HBM2DDL_DATABASE_ACTION, value = "create-drop")
		},
		settingProviders = @SettingProvider(settingName = CurrentTimestampGeneration.CLOCK_SETTING_NAME, provider = MutableClockSettingProvider.class)
)
public class TenantIdTest implements SessionFactoryProducer {

	String currentTenant;
	MutableClock clock;

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		clock = CurrentTimestampGeneration.getClock( scope.getSessionFactory() );
		clock.reset();
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		// Use the root tenant to clean up all partitions
		currentTenant = "root";
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Override
	public SessionFactoryImplementor produceSessionFactory(MetadataImplementor model) {
		final SessionFactoryBuilder sessionFactoryBuilder = model.getSessionFactoryBuilder();
		sessionFactoryBuilder.applyCurrentTenantIdentifierResolver( new CurrentTenantIdentifierResolver<String>() {
			@Override
			public String resolveCurrentTenantIdentifier() {
				return currentTenant;
			}
			@Override
			public boolean validateExistingCurrentSessions() {
				return false;
			}

			@Override
			public boolean isRoot(String tenantId) {
				return "root".equals( tenantId );
			}
		} );
		return (SessionFactoryImplementor) sessionFactoryBuilder.build();
	}

	@Test
	public void test(SessionFactoryScope scope) {
		currentTenant = "mine";
		Client client = new Client("Gavin");
		Account acc = new Account(client);
		scope.inTransaction( session -> {
			session.persist(client);
			session.persist(acc);
		} );
		scope.inTransaction( session -> {
			assertNotNull( session.find(Account.class, acc.id) );
			assertEquals( 1, session.createQuery("from Account", Account.class).getResultList().size() );
		} );
		assertEquals("mine", acc.tenantId);

		currentTenant = "yours";
		scope.inTransaction( session -> {
			//HHH-16830 Sessions applies tenantId filter on find()
			assertNull( session.find(Account.class, acc.id) );
			assertEquals( 0, session.createQuery("from Account", Account.class).getResultList().size() );
			session.disableFilter(TenantIdBinder.FILTER_NAME);
			assertNotNull( session.find(Account.class, acc.id) );
			assertEquals( 1, session.createQuery("from Account", Account.class).getResultList().size() );
		} );
	}

	@Test
	public void testRoot(SessionFactoryScope scope) {
		currentTenant = "root";
		scope.inTransaction( session -> {
			assertEquals( 0, session.createQuery( "from Account", Account.class ).getResultList().size() );
		} );

		currentTenant = "mine";
		Client client = new Client( "Gavin" );
		Account acc = new Account( client );
		scope.inTransaction( session -> {
			session.persist( client );
			session.persist( acc );
		} );
		assertEquals( "mine", acc.tenantId );
		scope.inTransaction( session -> {
			assertNotNull( session.find( Account.class, acc.id ) );
			assertEquals( 1, session.createQuery( "from Account", Account.class ).getResultList().size() );
		} );

		currentTenant = "root";
		// Root tenants should find entities from other tenants
		scope.inTransaction( session -> {
			assertNotNull( session.find( Account.class, acc.id ) );
			assertEquals( 1, session.createQuery( "from Account", Account.class ).getResultList().size() );
		} );

		// Root tenants should find entities from their own tenant
		Client rootClient = new Client( "Sacha" );
		Account rootAcc = new Account( rootClient );
		scope.inTransaction( session -> {
			session.persist( rootClient );
			session.persist( rootAcc );
		} );
		assertEquals( "root", rootAcc.tenantId );
		scope.inTransaction( session -> {
			assertNotNull( session.find( Account.class, rootAcc.id ) );
			assertEquals( 2, session.createQuery( "from Account", Account.class ).getResultList().size() );
		} );
	}

	@Test
	public void testErrorOnInsert(SessionFactoryScope scope) {
		currentTenant = "mine";
		Client client = new Client("Gavin");
		Account acc = new Account(client);
		acc.tenantId = "yours";
		try {
			scope.inTransaction( session -> {
				session.persist(client);
				session.persist(acc);
			} );
			fail("should have thrown");
		}
		catch (Throwable e) {
			assertTrue( e instanceof PropertyValueException );
		}
	}

	@Test
	public void testErrorOnUpdate(SessionFactoryScope scope) {
		currentTenant = "mine";
		Client client = new Client("Gavin");
		Account acc = new Account(client);
		scope.inTransaction( session -> {
			session.persist(client);
			session.persist(acc);
			acc.tenantId = "yours";
			client.tenantId = "yours";
			client.name = "Steve";
		} );
		//TODO: it would be better if this were an error
		scope.inTransaction( session -> {
			Account account = session.find(Account.class, acc.id);
			assertNotNull(account);
			assertEquals( "mine", acc.tenantId );
			assertEquals( "Steve", acc.client.name );
			assertEquals( "mine", acc.client.tenantId );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = SybaseASEDialect.class,
			reason = "low timestamp precision on Sybase")
	public void testEmbeddedTenantId(SessionFactoryScope scope) {
		currentTenant = "mine";
		Record record = new Record();
		scope.inTransaction( s -> s.persist( record ) );
		assertEquals( "mine", record.state.tenantId );
		assertNotNull( record.state.updated );

		clock.tick();

		scope.inTransaction( s -> {
			Record r = s.find( Record.class, record.id );
			assertEquals( "mine", r.state.tenantId );
			assertEquals( record.state.updated, r.state.updated );
			assertEquals( false, r.state.deleted );
			r.state.deleted = true;
		} );
		scope.inTransaction( s -> {
			Record r = s.find( Record.class, record.id );
			assertEquals( "mine", r.state.tenantId );
			assertNotEquals( record.state.updated, r.state.updated );
			assertEquals( true, r.state.deleted );
		} );
	}

	@Test
	public void testEntityManagerHint(SessionFactoryScope scope) {
		currentTenant = "mine";
		Record record = new Record();
		scope.inTransaction( s -> s.persist( record ) );
		assertEquals( "mine", record.state.tenantId );
		assertNotNull( record.state.updated );

		currentTenant = "yours";
		Record record2 = new Record();
		scope.inTransaction( s -> s.persist( record2 ) );
		assertEquals( "yours", record2.state.tenantId );
		assertNotNull( record2.state.updated );

		currentTenant = null;
		final EntityManagerFactory emf = scope.getSessionFactory();
		try (EntityManager em = emf.createEntityManager( Map.of( HINT_TENANT_ID, "mine" ) ) ) {
			Record r = em.find( Record.class, record.id );
			assertEquals( "mine", r.state.tenantId );

			// HHH-16830 Session applies tenant-id on #find
			Record yours = em.find( Record.class, record2.id );
			assertNull(yours);


			em.createQuery( "from Record where id = :id", Record.class )
					.setParameter( "id", record.id )
					.getSingleResult();
			assertEquals( "mine", r.state.tenantId );

			// However, Session does seem to apply tenant-id on queries
			try {
				em.createQuery( "from Record where id = :id", Record.class )
						.setParameter( "id", record2.id )
						.getSingleResult();
				fail( "Expecting an exception" );
			}
			catch (Exception expected) {
			}
		}
		catch (RuntimeException e) {
			currentTenant = "yours";
			scope.inTransaction( (s) -> s.createMutationQuery( "delete Record" ) );

			throw e;
		}
		finally {
			// for cleanup
			currentTenant = "mine";
		}
	}


	@Test
	public void tenantFilterWithStatelessSession(SessionFactoryScope scope) {
		currentTenant = "mine";
		Record myRecord1 = new Record();
		Record myRecord2 = new Record();

		scope.inTransaction( session -> {
			session.persist(myRecord1);
			session.persist(myRecord2);
		} );
		scope.inStatelessTransaction( session -> {
			assertThat( listAllRecordsForTenant( session ) ).hasSize( 2 );
		} );

		currentTenant = "yours";
		scope.inStatelessTransaction( session -> {
			assertThat( listAllRecordsForTenant( session ) ).isEmpty();
		} );
	}

	@Test
	@JiraKey( value = "HHH-17972")
	public void testChangeTenantId(SessionFactoryScope scope) {
		currentTenant = "mine";
		scope.inSession(
				session -> {
					Query<Client> sessionQuery = session.createQuery( "from Client", Client.class );

					Transaction t = session.beginTransaction();
					session.persist( new Client("Gavin") );
					t.commit();
					assertEquals(1, sessionQuery.getResultList().size() );
					assertEquals( "mine", sessionQuery.getResultList().get( 0 ).tenantId );

					Session newSession = session.sessionWithOptions().tenantIdentifier( "yours" ).connection().openSession();
					Query<Client> newSessionQuery = newSession.createQuery( "from Client", Client.class );
					t = newSession.beginTransaction();
					newSession.persist( new Client("Jan") );
					t.commit();

					assertEquals(1, newSessionQuery.getResultList().size() );
					assertEquals( "yours", newSessionQuery.getResultList().get( 0 ).tenantId );

					session.disableFilter( TenantIdBinder.FILTER_NAME );
					assertEquals(2, sessionQuery.getResultList().size() );

					newSession.disableFilter( TenantIdBinder.FILTER_NAME );
					assertEquals(2, newSessionQuery.getResultList().size() );

					newSession.close();
				}
		);
	}

	private static List<Record> listAllRecordsForTenant(StatelessSession session) {
		HibernateCriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
		JpaCriteriaQuery<Record> criteriaQuery = criteriaBuilder.createQuery( Record.class );
		JpaRoot<Record> from = criteriaQuery.from( Record.class );
		return session.createQuery( criteriaQuery ).getResultList();
	}
}
