/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.collections.type;

import java.util.Date;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.java.JdbcTimestampJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.BigIntJdbcTypeDescriptor;

/**
 * @author Vlad Mihalcea
 */
//tag::collections-map-custom-key-type-mapping-example[]

public class TimestampEpochType extends AbstractSingleColumnStandardBasicType<Date> {

    public static final TimestampEpochType INSTANCE = new TimestampEpochType();

    public TimestampEpochType() {
        super(
                BigIntJdbcTypeDescriptor.INSTANCE,
                JdbcTimestampJavaTypeDescriptor.INSTANCE
        );
    }

    @Override
    public String getName() {
        return "epoch";
    }

}
//end::collections-map-custom-key-type-mapping-example[]
