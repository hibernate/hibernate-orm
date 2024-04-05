/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


@JiraKey("HHH-13380")
@DomainModel(
        annotatedClasses = {
                LazyOneToManyWithEqualsImplementationTest.Person.class, LazyOneToManyWithEqualsImplementationTest.Course.class
        }
)
@SessionFactory
@BytecodeEnhanced
public class LazyOneToManyWithEqualsImplementationTest {

    private Long personId;

    @BeforeEach
    public void setUp(SessionFactoryScope scope) {
        scope.inTransaction( entityManager -> {
            Person p = new Person();
            entityManager.persist(p);
            personId = p.getId();

            Course c1 = new Course( "First Course", p );
            p.getCourses().add( c1 );
            entityManager.persist( c1 );

            Course c2 = new Course("Second Course", p );
            p.getCourses().add( c2 );
            entityManager.persist( c2 );
        });
    }


    @Test
    public void testRetrievalOfOneToMany(SessionFactoryScope scope) {
        scope.inTransaction( entityManager -> {
            Person p = entityManager.find( Person.class, personId );

            Set<Course> courses = p.getCourses();
            Assertions.assertEquals( courses.size(), 2 );
        });
    }

    @Entity(name = "Person")
    public static class Person {

        @Id
        @GeneratedValue()
        private Long id;
        public Long getId() {
            return id;
        }

        @OneToMany(mappedBy="person", fetch = FetchType.LAZY)
        private Set<Course> courses = new HashSet<>();
        public Set<Course> getCourses() { return courses; }

    }

    @Entity(name = "Course")
    public static class Course {

        @Id
        @GeneratedValue()
        private Long id;
        public Long getId() { return id; }

        @Basic
        private String title;
        public String getTitle() { return title; }

        @ManyToOne(fetch = FetchType.LAZY)
        @LazyToOne(LazyToOneOption.NO_PROXY)
        private Person person;
        public Person getPerson() { return person; }

        protected Course() {}
        public Course(String title, Person person) {
            this.title = title;
            this.person = person;
        }

        @Override
        public boolean equals(Object o) {
            if ( this == o ) return true;
            if ( o == null || getClass() != o.getClass() ) return false;
            Course course = (Course) o;
            return title.equals( course.title ) &&
                    person.equals( course.person );
        }

        @Override
        public int hashCode() {
            return Objects.hash( title, person );
        }
    }

}
