/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tenantid;

import org.hibernate.HibernateError;
import org.hibernate.PropertyValueException;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryProducer;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.binder.internal.TenantIdBinder;

import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION;
import static org.hibernate.internal.util.collections.CollectionHelper.toMap;
import static org.hibernate.jpa.HibernateHints.HINT_TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SessionFactory
@DomainModel(annotatedClasses = { Account.class, Client.class, Record.class })
@ServiceRegistry(
        settings = {
                @Setting(name = JAKARTA_HBM2DDL_DATABASE_ACTION, value = "create-drop")
        }
)
public class TenantIdTest implements SessionFactoryProducer {

    String currentTenant;

    @AfterEach
    public void cleanup(SessionFactoryScope scope) {
        scope.inTransaction( session -> {
            session.createQuery("delete from Account").executeUpdate();
            session.createQuery("delete from Client").executeUpdate();
        });
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
            assertEquals( 1, session.createQuery("from Account").getResultList().size() );
        } );
        assertEquals("mine", acc.tenantId);

        currentTenant = "yours";
        scope.inTransaction( session -> {
            assertNotNull( session.find(Account.class, acc.id) );
            assertEquals( 0, session.createQuery("from Account").getResultList().size() );
            session.disableFilter(TenantIdBinder.FILTER_NAME);
            assertNotNull( session.find(Account.class, acc.id) );
            assertEquals( 1, session.createQuery("from Account").getResultList().size() );
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

        //We need to wait a little to make sure the timestamps produced are different
        waitALittle();

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
        try (EntityManager em = emf.createEntityManager( toMap( HINT_TENANT_ID, "mine" ) ) ) {
            Record r = em.find( Record.class, record.id );
            assertEquals( "mine", r.state.tenantId );

            // Session seems to not apply tenant-id on #find
            Record yours = em.find( Record.class, record2.id );
            assertEquals( "yours", yours.state.tenantId );


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

    private static void waitALittle() {
        try {
            Thread.sleep( 10 );
        }
        catch (InterruptedException e) {
            throw new HibernateError( "Unexpected wakeup from test sleep" );
        }
    }
}
