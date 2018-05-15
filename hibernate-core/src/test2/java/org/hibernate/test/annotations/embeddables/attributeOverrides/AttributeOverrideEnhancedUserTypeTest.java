package org.hibernate.test.annotations.embeddables.attributeOverrides;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.IntegerType;
import org.hibernate.usertype.UserType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author András Eisenberger
 */
public class AttributeOverrideEnhancedUserTypeTest extends BaseNonConfigCoreFunctionalTestCase {
    @Override
    protected Class[] getAnnotatedClasses() {
        return new Class[] {
            TypeValue.class,
            AggregatedTypeValue.class
        };
    }

    @Test
    @TestForIssue( jiraKey = "HHH-11465" )
    public void testIt() {
        AggregatedTypeValue _e1 = doInHibernate( this::sessionFactory, session -> {
            AggregatedTypeValue e1 = new AggregatedTypeValue();
            e1.id = 1L;
            TypeValue t1 = new TypeValue();
            t1.time = YearMonth.of(2017, 5);
            e1.oneValue = t1;
            TypeValue t2 = new TypeValue();
            t2.time = YearMonth.of(2016, 4);
            e1.otherValue = t2;
            session.save( e1 );

            return e1;
        } );

        doInHibernate( this::sessionFactory, session -> {
            AggregatedTypeValue e1 = session.get( AggregatedTypeValue.class, _e1.id );
            assertEquals(e1.oneValue.time, YearMonth.of(2017, 5));
            assertEquals(e1.otherValue.time, YearMonth.of(2016, 4));
            session.delete( e1 );
        } );
    }

    @Embeddable
    public static class TypeValue {
        @Type(type = "year_month")
        @Columns(columns = {
                @Column(name = "year", nullable = true),
                @Column(name = "month", nullable = true)
        })
        YearMonth time;
    }

    @Entity
    @Table( name = "AGG_TYPE" )
    @TypeDef(
        name = "year_month",
        typeClass = YearMonthUserType.class
    )
    public static class AggregatedTypeValue {
        @Id
        private Long id;

        @Embedded
        @AttributeOverrides({
                @AttributeOverride(name = "time", column = @Column(name = "one_year")),
                @AttributeOverride(name = "time", column = @Column(name = "one_month"))
        })
        private TypeValue oneValue;

        @Embedded
        @AttributeOverrides({
                @AttributeOverride(name = "time", column = @Column(name = "other_year")),
                @AttributeOverride(name = "time", column = @Column(name = "other_month"))
        })
        private TypeValue otherValue;
    }

	public static class YearMonthUserType implements UserType, Serializable {
		@Override
		public int[] sqlTypes() {
			return new int[]{
                IntegerType.INSTANCE.sqlType(),
                IntegerType.INSTANCE.sqlType(),
			};
		}

		@Override
		public Class returnedClass() {
			return YearMonth.class;
		}

		@Override
		public boolean equals(final Object x, final Object y) throws HibernateException {
			if (x == y) {
				return true;
			}
			if (x == null || y == null) {
				return false;
			}
			final YearMonth mtx = (YearMonth) x;
			final YearMonth mty = (YearMonth) y;
			return mtx.equals(mty);
		}

		@Override
		public int hashCode(final Object x) throws HibernateException {
			return x.hashCode();
		}

		@Override
		public Object nullSafeGet(final ResultSet rs, final String[] names, final SharedSessionContractImplementor session, final Object owner) throws HibernateException, SQLException {
			assert names.length == 2;
			final Integer year = IntegerType.INSTANCE.nullSafeGet(rs, names[0], session);
			final Integer month = IntegerType.INSTANCE.nullSafeGet(rs, names[1], session);
			return year == null || month == null ? null : YearMonth.of(year, month);
		}

		@Override
		public void nullSafeSet(final PreparedStatement st, final Object value, final int index, final SharedSessionContractImplementor session) throws HibernateException, SQLException {
			if (value == null) {
				IntegerType.INSTANCE.set(st, null, index, session);
				IntegerType.INSTANCE.set(st, null, index + 1, session);
			} else {
				final YearMonth YearMonth = (YearMonth) value;

				IntegerType.INSTANCE.set(st, YearMonth.getYear(), index, session);
				IntegerType.INSTANCE.set(st, YearMonth.getMonthValue(), index + 1, session);
			}
		}

		@Override
		public Object deepCopy(final Object value) throws HibernateException {
			return value;
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		public Serializable disassemble(final Object value) throws HibernateException {
			return (Serializable) value;
		}

		@Override
		public Object assemble(final Serializable cached, final Object value) throws HibernateException {
			return cached;
		}

		@Override
		public Object replace(final Object original, final Object target, final Object owner) throws HibernateException {
			return original;
		}
	}
}
