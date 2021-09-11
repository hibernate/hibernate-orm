/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.tenantid;

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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.AvailableSettings.HBM2DDL_DATABASE_ACTION;
import static org.junit.jupiter.api.Assertions.*;

@SessionFactory
@DomainModel(annotatedClasses = { Account.class, Client.class })
@ServiceRegistry(
        settings = {
                @Setting(name = HBM2DDL_DATABASE_ACTION, value = "create-drop")
        }
)
public class TenantIdTest implements SessionFactoryProducer {

    String currentTenant;

    @Override
    public SessionFactoryImplementor produceSessionFactory(MetadataImplementor model) {
        final SessionFactoryBuilder sessionFactoryBuilder = model.getSessionFactoryBuilder();
        sessionFactoryBuilder.applyCurrentTenantIdentifierResolver( new CurrentTenantIdentifierResolver() {
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
            assertNull( session.find(Account.class, acc.id) );
            assertEquals( 0, session.createQuery("from Account").getResultList().size() );
        } );
    }

    @Test
    public void testError(SessionFactoryScope scope) {
        currentTenant = "mine";
        Client client = new Client("Gavin");
        Account acc = new Account();
        acc.tenantId = "yours";
        try {
            scope.inTransaction( session -> {
                session.persist(client);
                session.persist(acc);
            } );
            fail("should have thrown");
        }
        catch (Throwable e) {
            assertTrue( e.getCause() instanceof PropertyValueException );
        }
    }
}
