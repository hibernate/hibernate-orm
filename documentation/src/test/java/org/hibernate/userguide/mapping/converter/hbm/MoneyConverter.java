/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.converter.hbm;

import javax.persistence.AttributeConverter;

//tag::basic-hbm-attribute-converter-mapping-moneyconverter-example[]
public class MoneyConverter
        implements AttributeConverter<Money, Long> {

    @Override
    public Long convertToDatabaseColumn(Money attribute) {
        return attribute == null ? null : attribute.getCents();
    }

    @Override
    public Money convertToEntityAttribute(Long dbData) {
        return dbData == null ? null : new Money( dbData );
    }
}
//end::basic-hbm-attribute-converter-mapping-moneyconverter-example[]
