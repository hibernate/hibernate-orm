/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.collectionbasictype;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.MutableMutabilityPlan;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Vlad Mihalcea
 */
public class CommaDelimitedStringMapJavaTypeDescriptor extends AbstractBasicJavaDescriptor<Map> {

    public static final String DELIMITER = ",";

    public CommaDelimitedStringMapJavaTypeDescriptor() {
        super(
            Map.class,
            new MutableMutabilityPlan<Map>() {
                @Override
                protected Map deepCopyNotNull(Map value) {
                    return new HashMap( value );
                }
            }
        );
    }

    @Override
    public String toString(Map value) {
        return null;
    }

    @Override
    public Map fromString(String string) {
        return null;
    }

    @Override
    public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
        return context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( Types.VARCHAR );
    }

    @Override
    public <X> X unwrap(Map value, Class<X> type, SharedSessionContractImplementor session) {
        return (X) toString( value );
    }

    @Override
    public <X> Map wrap(X value, SharedSessionContractImplementor session) {
        return fromString( (String) value );
    }
}
