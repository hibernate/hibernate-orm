/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.filter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
        annotatedClasses = {
                FilterDefinitionOrderTest.AMyEntity.class,
                FilterDefinitionOrderTest.XEntity.class
        }
)
@JiraKey("HHH-19036")
// Real test is if it actually starts, as that's where the filter binding happens,
// but let's also check that it was actually processed as well:
public class FilterDefinitionOrderTest extends AbstractStatefulStatelessFilterTest {

    @BeforeEach
    void setUp() {
        scope.inTransaction(session -> {
            AMyEntity entity1 = new AMyEntity();
            entity1.setField("test");
            AMyEntity entity2 = new AMyEntity();
            entity2.setField("Hello");
            session.persist(entity1);
            session.persist(entity2);
        });
    }

    @AfterEach
    void tearDown() {
        scope.inTransaction(session -> {
            session.createMutationQuery("delete from AMyEntity").executeUpdate();
        });
    }

    @ParameterizedTest
    @MethodSource("transactionKind")
    void smokeTest(BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
        inTransaction.accept(scope, session -> {
            session.enableFilter("x_filter");
            List<AMyEntity> entities = session.createQuery("FROM AMyEntity", AMyEntity.class).getResultList();
            assertThat(entities).hasSize(1)
                    .allSatisfy(entity -> assertThat(entity.getField()).isEqualTo("Hello"));

            session.disableFilter("x_filter");
            entities = session.createQuery("FROM AMyEntity", AMyEntity.class).getResultList();
            assertThat(entities).hasSize(2);
        });
    }

    @Entity(name = "AMyEntity")
    @Filter(name = "x_filter")
    public static class AMyEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        @Column(nullable = false)
        private Long id;

        public String field;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }
    }

    @Entity(name = "XEntity")
    @FilterDef(name = "x_filter", defaultCondition = "field = 'Hello'")
    public static class XEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        @Column(nullable = false)
        private Long id;

        public String field;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }
    }
}
