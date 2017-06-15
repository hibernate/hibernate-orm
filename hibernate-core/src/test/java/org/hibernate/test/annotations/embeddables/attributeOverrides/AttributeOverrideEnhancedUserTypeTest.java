package org.hibernate.test.annotations.embeddables.attributeOverrides;

import org.hibernate.Session;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Type;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import javax.persistence.*;
import java.time.YearMonth;

import static org.junit.Assert.assertEquals;

/**
 * @author András Eisenberger
 */
public class AttributeOverrideEnhancedUserTypeTest extends BaseNonConfigCoreFunctionalTestCase {
    @Override
    protected Class[] getAnnotatedClasses() {
        return new Class[] { AttributeOverrideEnhancedUserTypeTest.TypeValue.class, AttributeOverrideEnhancedUserTypeTest.AggregatedTypeValue.class };
    }

    @Test
    @TestForIssue( jiraKey = "HHH-11465" )
    public void testIt() {
        Session s = openSession();
        s.beginTransaction();
        AggregatedTypeValue e1 = new AggregatedTypeValue();
        e1.id = 1l;
        TypeValue t1 = new TypeValue();
        t1.time = YearMonth.of(2017, 5);
        e1.oneValue = t1;
        TypeValue t2 = new TypeValue();
        t2.time = YearMonth.of(2016, 4);
        e1.otherValue = t2;
        s.save( e1 );
        s.getTransaction().commit();
        s.close();

        s = openSession();
        s.beginTransaction();
        e1 = s.get( AggregatedTypeValue.class, e1.id );
        assertEquals(e1.oneValue.time, YearMonth.of(2017, 5));
        assertEquals(e1.otherValue.time, YearMonth.of(2016, 4));
        s.delete( e1 );
        s.getTransaction().commit();
        s.close();
    }

    @Embeddable
    public static class TypeValue {
        @Type(type = "org.hibernate.test.annotations.embeddables.YearMonthUserType")
        @Columns(columns = {
                @Column(name = "year", nullable = true),
                @Column(name = "month", nullable = true)
        })
        YearMonth time;
    }

    @Entity
    @Table( name = "AGG_TYPE" )
    public static class AggregatedTypeValue {
        @Id
        private Long id;

        @Embedded
        @AttributeOverrides({
                @AttributeOverride(name = "time", column = @Column(name = "one_year")),
                @AttributeOverride(name = "time", column = @Column(name = "one_month"))
        })
        AttributeOverrideEnhancedUserTypeTest.TypeValue oneValue;

        @Embedded
        @AttributeOverrides({
                @AttributeOverride(name = "time", column = @Column(name = "other_year")),
                @AttributeOverride(name = "time", column = @Column(name = "other_month"))
        })
        AttributeOverrideEnhancedUserTypeTest.TypeValue otherValue;
    }
}
