/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.converter;

import java.time.Period;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PeriodStringTest extends BaseEntityManagerFunctionalTestCase {

    private Period period = Period.ofYears( 1 ).plusMonths( 2 ).plusDays( 3 );

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {
                Event.class
        };
    }

    @Test
    public void testLifecycle() {
        doInJPA( this::entityManagerFactory, entityManager -> {
            Event event = new Event( period );
            entityManager.persist( event );
        } );
        doInJPA( this::entityManagerFactory, entityManager -> {
            Event event = entityManager.createQuery( "from Event", Event.class ).getSingleResult();
            assertEquals( period, event.getSpan() );
        } );
        doInJPA( this::entityManagerFactory, entityManager -> {
            //tag::basic-jpa-convert-period-string-converter-immutability-plan-example[]
            Event event = entityManager.createQuery( "from Event", Event.class ).getSingleResult();
            event.setSpan(Period
                .ofYears( 3 )
                .plusMonths( 2 )
                .plusDays( 1 )
            );
            //end::basic-jpa-convert-period-string-converter-immutability-plan-example[]
        } );
    }

    //tag::basic-jpa-convert-period-string-converter-mapping-example[]
    @Entity(name = "Event")
    public static class Event {

        @Id
        @GeneratedValue
        private Long id;

        @Convert(converter = PeriodStringConverter.class)
        @Column(columnDefinition = "")
        private Period span;

        //Getters and setters are omitted for brevity

    //end::basic-jpa-convert-period-string-converter-mapping-example[]

        public Event() {
        }

        public Event(Period span) {
            this.span = span;
        }

        public Long getId() {
            return id;
        }

        public Period getSpan() {
            return span;
        }

        public void setSpan(Period span) {
            this.span = span;
        }

        //tag::basic-jpa-convert-period-string-converter-mapping-example[]
    }
    //end::basic-jpa-convert-period-string-converter-mapping-example[]
}
