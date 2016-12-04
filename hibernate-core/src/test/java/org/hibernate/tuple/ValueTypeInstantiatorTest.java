package org.hibernate.tuple;

import java.util.Arrays;
import java.util.Objects;

import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.type.Type;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ValueTypeInstantiatorTest {

	@Mock
	private Component component;

	@Test
	public void shouldCreateValueTypeInstantiatorForValueType() {

		// given
		givenProperties(
				givenProperty( "firstProperty", String.class ),
				givenProperty( "secondProperty", Integer.class )
		);
		given( component.getComponentClass() ).willReturn( ValueType.class );

		// when
		ValueTypeInstantiator actual = ValueTypeInstantiator.of( component )
				.orElse( null );

		// then
		then( actual ).isNotNull();
	}

	@Test
	public void shouldUseConstructorWithExactParameterTypeAndName() {

		// given
		givenProperties(
				givenProperty( "firstProperty", String.class ),
				givenProperty( "secondProperty", Integer.class )
		);
		given( component.getComponentClass() ).willReturn( ValueType.class );
		ValueTypeInstantiator valueTypeInstantiator = ValueTypeInstantiator.of( component )
				.orElse( null );
		String[] propertyNames = { "firstProperty", "secondProperty" };
		Object[] givenArgs = { "givenFirstProperty", 1 };

		// when
		Object actual = valueTypeInstantiator.instantiate( propertyNames, givenArgs );

		// then
		then( actual ).isEqualTo( new ValueType( (String) givenArgs[0], (Integer) givenArgs[1] ) );
	}

	private void givenProperties(Property... properties) {
		given( component.getPropertyIterator() ).willReturn( Arrays.asList( properties ).iterator() );
	}

	private Property givenProperty(String name, Class type) {
		Property property = mock( Property.class );
		when( property.getName() ).thenReturn( name );
		given( property.getName() ).willReturn( name );
		Type mockedType = mock( Type.class );
		given( mockedType.getReturnedClass() ).willReturn( type );
		given( property.getType() ).willReturn( mockedType );
		return property;
	}

	static class ValueType {
		private final String firstProperty;
		private final Integer secondProperty;

		ValueType() {
			throw new UnsupportedOperationException();
		}

		ValueType(Integer secondProperty) {
			throw new UnsupportedOperationException();
		}

		ValueType(String firstProperty) {
			throw new UnsupportedOperationException();
		}

		ValueType(Integer firstProperty, Integer secondProperty) {
			throw new UnsupportedOperationException();
		}

		ValueType(String firstProperty, Integer secondProperty) {
			this.firstProperty = firstProperty;
			this.secondProperty = secondProperty;
		}

		ValueType(String firstProperty, String secondProperty) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			ValueType valueType = (ValueType) o;
			return Objects.equals( firstProperty, valueType.firstProperty ) &&
					Objects.equals( secondProperty, valueType.secondProperty );
		}

		@Override
		public int hashCode() {
			return Objects.hash( firstProperty, secondProperty );
		}
	}
}