/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.converter;

import java.time.Period;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * @author Vlad Mihalcea
 */
//tag::basic-jpa-convert-period-string-converter-example[]
@Converter
public class PeriodStringConverter
        implements AttributeConverter<Period, String> {

    @Override
    public String convertToDatabaseColumn(Period attribute) {
        return attribute.toString();
    }

    @Override
    public Period convertToEntityAttribute(String dbData) {
        return Period.parse( dbData );
    }
}
//end::basic-jpa-convert-period-string-converter-example[]
