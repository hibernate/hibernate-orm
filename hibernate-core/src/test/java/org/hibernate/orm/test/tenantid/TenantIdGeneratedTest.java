/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tenantid;

import org.hibernate.annotations.TenantId;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryProducer;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_DATABASE_ACTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SessionFactory
@DomainModel(annotatedClasses = { TenantIdGeneratedTest.DisplayIdBE.class })
@ServiceRegistry(
        settings = {
                @Setting(name = JAKARTA_HBM2DDL_DATABASE_ACTION, value = "create-drop")
        }
)
@Jira("https://hibernate.atlassian.net/browse/HHH-19004")
public class TenantIdGeneratedTest implements SessionFactoryProducer {

    @AfterEach
    public void cleanup(SessionFactoryScope scope) {
        scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
    }

    @Override
    public SessionFactoryImplementor produceSessionFactory(MetadataImplementor model) {
        final SessionFactoryBuilder sessionFactoryBuilder = model.getSessionFactoryBuilder();
        sessionFactoryBuilder.applyCurrentTenantIdentifierResolver( new CurrentTenantIdentifierResolver<Long>() {
            @Override
            public Long resolveCurrentTenantIdentifier() {
                return 0L;
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
        scope.inTransaction( session -> {
            session.persist(new DisplayIdBE( new DisplayIdKeyBE( null, DisplayIdType.TYPE1 ), 1L ));
        } );
        scope.inTransaction( session -> {
            assertNotNull( session.find( DisplayIdBE.class, new DisplayIdKeyBE( 0L, DisplayIdType.TYPE1 ) ) );
            assertEquals( 1, session.createQuery("from DisplayIdBE", DisplayIdBE.class).getResultList().size() );
        } );
    }

    @Entity(name = "DisplayIdBE")
    public static class DisplayIdBE {

        @EmbeddedId
        private DisplayIdKeyBE id;

        @Column(name = "display_id_value", nullable = false)
        private long displayIdValue;

        protected DisplayIdBE() {
        }

        public DisplayIdBE(DisplayIdKeyBE id, long displayIdValue) {
            this.id = id;
            this.displayIdValue = displayIdValue;
        }
    }

    public enum DisplayIdType {
        TYPE1,
        TYPE2
    }

    @Embeddable
    public static class DisplayIdKeyBE {

        @TenantId
        @Column(name = "tenant_id", nullable = false)
        private Long tenantId;

        @Column(name = "type", nullable = false)
        @Enumerated(EnumType.STRING)
        private DisplayIdType type;

        protected DisplayIdKeyBE() {}

        public DisplayIdKeyBE(Long tenantId, DisplayIdType type) {
            this.tenantId = tenantId;
            this.type = type;
        }
    }

}
