/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.collectionbasictype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.MutableMutabilityPlan;

/**
 * @author Vlad Mihalcea
 */
public class CommaDelimitedStringMapJavaTypeDescriptor extends AbstractTypeDescriptor<Map> {

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
    public <X> X unwrap(Map value, Class<X> type, WrapperOptions options) {
        return (X) toString( value );
    }

    @Override
    public <X> Map wrap(X value, WrapperOptions options) {
        return fromString( (String) value );
    }
}
