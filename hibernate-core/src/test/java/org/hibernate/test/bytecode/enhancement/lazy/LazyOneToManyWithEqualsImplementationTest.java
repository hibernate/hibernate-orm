/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

@TestForIssue(jiraKey = "HHH-13380")
@RunWith( BytecodeEnhancerRunner.class )
public class LazyOneToManyWithEqualsImplementationTest
        extends BaseEntityManagerFunctionalTestCase {

    private Long personId;

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] { Person.class, Course.class };
    }

    @Before
    public void setUp() {
        doInJPA( this::entityManagerFactory, entityManager -> {
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
    public void testRetrievalOfOneToMany() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            Person p = entityManager.find( Person.class, personId );

            Set<Course> courses = p.getCourses();
            assertEquals( courses.size(), 2 );
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
