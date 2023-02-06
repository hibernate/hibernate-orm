package org.hibernate.orm.test.annotations;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.orm.test.query.criteria.internal.hhh14197.AbstractPersistent;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

@TestForIssue( jiraKey = "HHH-16122" )
public class HHH16122Test extends BaseEntityManagerFunctionalTestCase {
    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class[] { ValueConverter.class, SuperClass.class, SubClass.class };
    }

    @Test
    public void testGenericSuperClassWithConverter() {
        // The test is successful if the entity manager factory can be built.
    }

    public record ConvertedValue(long value) {}

    @Converter(autoApply = true)
    public static class ValueConverter implements AttributeConverter<ConvertedValue, Long> {
        @Override
        public Long convertToDatabaseColumn( ConvertedValue value ) {
            return value.value;
        }
        @Override
        public ConvertedValue convertToEntityAttribute( Long value ) {
            return new ConvertedValue(value);
        }
    }

    @MappedSuperclass
    public static abstract class SuperClass<S extends SuperClass> extends AbstractPersistent {
        public ConvertedValue convertedValue = new ConvertedValue( 1 );
        public ConvertedValue getConvertedValue() {
            return convertedValue;
        }
        public void setConvertedValue(ConvertedValue convertedValue) {
            this.convertedValue = convertedValue;
        }
    }

    @Entity(name = "SubClass")
    public static class SubClass extends SuperClass<SubClass> {}
}
