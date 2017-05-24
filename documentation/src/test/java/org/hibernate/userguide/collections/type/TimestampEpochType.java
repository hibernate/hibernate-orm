/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.collections.type;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.Date;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.LiteralType;
import org.hibernate.type.StringType;
import org.hibernate.type.VersionType;
import org.hibernate.type.descriptor.java.JdbcTimestampTypeDescriptor;
import org.hibernate.type.descriptor.sql.BigIntTypeDescriptor;

/**
 * @author Vlad Mihalcea
 */
//tag::collections-map-custom-key-type-mapping-example[]

public class TimestampEpochType
        extends AbstractSingleColumnStandardBasicType<Date>
        implements VersionType<Date>, LiteralType<Date> {

    public static final TimestampEpochType INSTANCE = new TimestampEpochType();

    public TimestampEpochType() {
        super(
            BigIntTypeDescriptor.INSTANCE,
            JdbcTimestampTypeDescriptor.INSTANCE
        );
    }

    @Override
    public String getName() {
        return "epoch";
    }

    @Override
    public Date next(
        Date current,
        SharedSessionContractImplementor session) {
        return seed( session );
    }

    @Override
    public Date seed(
        SharedSessionContractImplementor session) {
        return new Timestamp( System.currentTimeMillis() );
    }

    @Override
    public Comparator<Date> getComparator() {
        return getJavaTypeDescriptor().getComparator();
    }

    @Override
    public String objectToSQLString(
        Date value,
        Dialect dialect) throws Exception {
        final Timestamp ts = Timestamp.class.isInstance( value )
            ? ( Timestamp ) value
            : new Timestamp( value.getTime() );
        return StringType.INSTANCE.objectToSQLString(
            ts.toString(), dialect
        );
    }

    @Override
    public Date fromStringValue(
        String xml) throws HibernateException {
        return fromString( xml );
    }
}
//end::collections-map-custom-key-type-mapping-example[]
