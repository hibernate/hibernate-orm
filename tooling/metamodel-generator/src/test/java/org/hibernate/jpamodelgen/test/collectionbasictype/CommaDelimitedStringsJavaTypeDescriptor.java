/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.collectionbasictype;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.MutableMutabilityPlan;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Vlad Mihalcea
 */
public class CommaDelimitedStringsJavaTypeDescriptor extends AbstractBasicJavaDescriptor<List> {

    public static final String DELIMITER = ",";

    public CommaDelimitedStringsJavaTypeDescriptor() {
        super(
            List.class,
            new MutableMutabilityPlan<List>() {
                @Override
                protected List deepCopyNotNull(List value) {
                    return new ArrayList( value );
                }
            }
        );
    }

    @Override
    public String toString(List value) {
        return ( (List<String>) value ).stream().collect( Collectors.joining( DELIMITER ) );
    }

    @Override
    public List fromString(String string) {
        List<String> values = new ArrayList<>();
        Collections.addAll( values, string.split( DELIMITER ) );
        return values;
    }

    @Override
    public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
        return context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( Types.VARCHAR );
    }

    @Override
    public <X> X unwrap(List value, Class<X> type, SharedSessionContractImplementor session) {
        return (X) toString( value );
    }

    @Override
    public <X> List wrap(X value, SharedSessionContractImplementor session) {
        return fromString( (String) value );
    }
}
