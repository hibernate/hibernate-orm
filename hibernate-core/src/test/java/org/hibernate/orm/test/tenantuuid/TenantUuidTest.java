/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tenantuuid;

import org.hibernate.PropertyValueException;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryProducer;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.binder.internal.TenantIdBinder;
import org.hibernate.testing.util.uuid.SafeRandomUUIDGenerator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SessionFactory
@DomainModel(annotatedClasses = { Account.class, Client.class })
@ServiceRegistry(
        settings = {
                @Setting(name = JAKARTA_HBM2DDL_DATABASE_ACTION, value = "create-drop")
        }
)
public class TenantUuidTest implements SessionFactoryProducer {

    private static final UUID mine = SafeRandomUUIDGenerator.safeRandomUUID();
    private static final UUID yours = SafeRandomUUIDGenerator.safeRandomUUID();

    UUID currentTenant;

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
        sessionFactoryBuilder.applyCurrentTenantIdentifierResolver( new CurrentTenantIdentifierResolver<UUID>() {
            @Override
            public UUID resolveCurrentTenantIdentifier() {
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
        currentTenant = mine;
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
        assertEquals(mine, acc.tenantId);

        currentTenant = yours;
        scope.inTransaction( session -> {
            //HHH-16830 Sessions applies tenantId filter on find()
            assertNull( session.find(Account.class, acc.id) );
            assertEquals( 0, session.createQuery("from Account").getResultList().size() );
            session.disableFilter(TenantIdBinder.FILTER_NAME);
            assertNotNull( session.find(Account.class, acc.id) );
            assertEquals( 1, session.createQuery("from Account").getResultList().size() );
        } );
    }

    @Test
    public void testErrorOnInsert(SessionFactoryScope scope) {
        currentTenant = mine;
        Client client = new Client("Gavin");
        Account acc = new Account(client);
        acc.tenantId = yours;
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
        currentTenant = mine;
        Client client = new Client("Gavin");
        Account acc = new Account(client);
        scope.inTransaction( session -> {
            session.persist(client);
            session.persist(acc);
            acc.tenantId = yours;
            client.tenantId = yours;
            client.name = "Steve";
        } );
        //TODO: it would be better if this were an error
        scope.inTransaction( session -> {
            Account account = session.find(Account.class, acc.id);
            assertNotNull(account);
            assertEquals( mine, acc.tenantId );
            assertEquals( "Steve", acc.client.name );
            assertEquals( mine, acc.client.tenantId );
        } );
    }
}
