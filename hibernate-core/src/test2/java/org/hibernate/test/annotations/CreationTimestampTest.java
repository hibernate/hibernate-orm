package org.hibernate.test.annotations;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Borys Piela
 */
public class CreationTimestampTest extends BaseEntityManagerFunctionalTestCase {

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{
                Event.class
        };
    }

    @Entity(name = "Event")
    private static class Event {

        @Id
        @GeneratedValue
        private Long id;

        @Column(name = "`date`")
        @CreationTimestamp
        private Date date;

        @Column(name = "`calendar`")
        @CreationTimestamp
        private Calendar calendar;

        @Column(name = "`sqlDate`")
        @CreationTimestamp
        private java.sql.Date sqlDate;

        @Column(name = "`time`")
        @CreationTimestamp
        private Time time;

        @Column(name = "`timestamp`")
        @CreationTimestamp
        private Timestamp timestamp;

        @Column(name = "`instant`")
        @CreationTimestamp
        private Instant instant;

        @Column(name = "`localDate`")
        @CreationTimestamp
        private LocalDate localDate;

        @Column(name = "`localDateTime`")
        @CreationTimestamp
        private LocalDateTime localDateTime;

        @Column(name = "`localTime`")
        @CreationTimestamp
        private LocalTime localTime;

        @Column(name = "`monthDay`")
        @CreationTimestamp
        private MonthDay monthDay;

        @Column(name = "`offsetDateTime`")
        @CreationTimestamp
        private OffsetDateTime offsetDateTime;

        @Column(name = "`offsetTime`")
        @CreationTimestamp
        private OffsetTime offsetTime;

        @Column(name = "`year`")
        @CreationTimestamp
        private Year year;

        @Column(name = "`yearMonth`")
        @CreationTimestamp
        private YearMonth yearMonth;

        @Column(name = "`zonedDateTime`")
        @CreationTimestamp
        private ZonedDateTime zonedDateTime;

        public Event() {
        }

        public Long getId() {
            return id;
        }

        public Date getDate() {
            return date;
        }

        public Calendar getCalendar() {
            return calendar;
        }

        public java.sql.Date getSqlDate() {
            return sqlDate;
        }

        public Time getTime() {
            return time;
        }

        public Timestamp getTimestamp() {
            return timestamp;
        }

        public Instant getInstant() {
            return instant;
        }

        public LocalDate getLocalDate() {
            return localDate;
        }

        public LocalDateTime getLocalDateTime() {
            return localDateTime;
        }

        public LocalTime getLocalTime() {
            return localTime;
        }

        public MonthDay getMonthDay() {
            return monthDay;
        }

        public OffsetDateTime getOffsetDateTime() {
            return offsetDateTime;
        }

        public OffsetTime getOffsetTime() {
            return offsetTime;
        }

        public Year getYear() {
            return year;
        }

        public YearMonth getYearMonth() {
            return yearMonth;
        }

        public ZonedDateTime getZonedDateTime() {
            return zonedDateTime;
        }
    }

    @Test
    public void generatesCurrentTimestamp() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            Event event = new Event();
            entityManager.persist(event);
            entityManager.flush();
            Assert.assertNotNull(event.getDate());
            Assert.assertNotNull(event.getCalendar());
            Assert.assertNotNull(event.getSqlDate());
            Assert.assertNotNull(event.getTime());
            Assert.assertNotNull(event.getTimestamp());
            Assert.assertNotNull(event.getInstant());
            Assert.assertNotNull(event.getLocalDate());
            Assert.assertNotNull(event.getLocalDateTime());
            Assert.assertNotNull(event.getLocalTime());
            Assert.assertNotNull(event.getMonthDay());
            Assert.assertNotNull(event.getOffsetDateTime());
            Assert.assertNotNull(event.getOffsetTime());
            Assert.assertNotNull(event.getYear());
            Assert.assertNotNull(event.getYearMonth());
            Assert.assertNotNull(event.getZonedDateTime());
        });
    }
}
