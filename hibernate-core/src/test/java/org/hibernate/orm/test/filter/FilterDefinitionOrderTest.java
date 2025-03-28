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
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

@DomainModel(
        annotatedClasses = {
                FilterDefinitionOrderTest.AMyEntity.class,
                FilterDefinitionOrderTest.XEntity.class
        }
)
@JiraKey("HHH-19036")
public class FilterDefinitionOrderTest extends AbstractStatefulStatelessFilterTest {

    @Test
    public void smokeTest() {
        // nothing really to test here,
        // if the test starts, then everything is great!
    }

    @Entity
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

    @Entity
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
