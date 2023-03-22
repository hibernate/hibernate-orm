package org.hibernate.orm.test.annotations.embeddables;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Query;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
        annotatedClasses = {
                EmbeddableIdNullableTest.Product.class
        }
)
@SessionFactory
@TestForIssue(jiraKey = "HHH-16356")
@ServiceRegistry(settings = @Setting( name = AvailableSettings.ID_NULLABLE, value = "true"))
public class EmbeddableIdNullableTest {

    @Test
    public void testSelectWithNullableIdComponent(SessionFactoryScope scope) {
        scope.inTransaction(
                session -> {
                    // Select the entity from the database using a null id component
                    Query query = session.createNativeQuery("SELECT 123 AS id, NULL AS name", Product.class);
                    List<Product> results = query.getResultList();

                    assertThat( results.size() ).isEqualTo(1);
                    assertThat( results.get(0).getId() ).isEqualTo(123L);
                    assertThat( results.get(0).getName() ).isNull();
                }
        );
    }

    @Entity(name = "Product")
    public static class Product implements Serializable {
        @Id
        private Long id;

        @Id
        private String name;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

}
