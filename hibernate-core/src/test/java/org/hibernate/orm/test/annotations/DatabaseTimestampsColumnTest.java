/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.dialect.Dialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.tuple.AnnotationValueGeneration;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.tuple.ValueGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Date;

@Jpa(annotatedClasses = DatabaseTimestampsColumnTest.Person.class)
public class DatabaseTimestampsColumnTest {

    @Entity(name = "Person")
    public class Person {

        @Id
        @GeneratedValue
        private Long id;

        @NaturalId(mutable = true)
        private String name;

        @Column(nullable = false)
        @Timestamp(GenerationTime.INSERT)
        private Date creationDate;

        @Column(nullable = true)
        @Timestamp(GenerationTime.UPDATE)
        private Date editionDate;

        @Column(nullable = false, name="version")
        @Timestamp(GenerationTime.ALWAYS)
        private Date timestamp;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Date getCreationDate() {
            return creationDate;
        }

        public Date getEditionDate() {
            return editionDate;
        }

        public Date getTimestamp() {
            return timestamp;
        }
    }

    @ValueGenerationType(generatedBy = TimestampValueGeneration.class)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Timestamp { GenerationTime value(); }

    public static class TimestampValueGeneration
            implements AnnotationValueGeneration<Timestamp> {

        private GenerationTiming timing;

        @Override
        public void initialize(Timestamp annotation, Class<?> propertyType) {
            timing = annotation.value().getEquivalent();
        }

        public GenerationTiming getGenerationTiming() {
            return timing;
        }

        public ValueGenerator<?> getValueGenerator() {
            return null;
        }

        public boolean referenceColumnInSql() {
            return true;
        }

        public String getDatabaseGeneratedReferencedColumnValue() {
            return "current_timestamp";
        }

        public String getDatabaseGeneratedReferencedColumnValue(Dialect dialect) {
            return dialect.currentTimestamp();
        }
    }

    @Test
    public void generatesCurrentTimestamp(EntityManagerFactoryScope scope) {
        scope.inEntityManager(
                entityManager -> {
                    entityManager.getTransaction().begin();
                    Person person = new Person();
                    person.setName("John Doe");
                    entityManager.persist(person);
                    entityManager.getTransaction().commit();
                    Date creationDate = person.getCreationDate();
                    Assertions.assertNotNull(creationDate);
                    Assertions.assertNull(person.getEditionDate());
                    Date timestamp = person.getTimestamp();
                    Assertions.assertNotNull(timestamp);

                    try { Thread.sleep(1_000); } catch (InterruptedException ie) {};

                    entityManager.getTransaction().begin();
                    person.setName("Jane Doe");
                    entityManager.getTransaction().commit();
                    Assertions.assertNotNull(person.getCreationDate());
                    Assertions.assertEquals(creationDate, person.getCreationDate());
                    Assertions.assertNotNull(person.getEditionDate());
                    Assertions.assertNotNull(person.getTimestamp());
                    Assertions.assertNotEquals(timestamp, person.getTimestamp());
                }
        );
    }
}
