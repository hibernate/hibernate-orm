package org.hibernate.testing.orm.assertj;

import java.lang.reflect.Field;

import org.assertj.core.api.AbstractClassAssert;

import static org.assertj.core.api.Assertions.assertThat;

/// Assertions for classes that include synthetic fields.
///
/// @author Steve Ebersole
public class ClassAssert extends AbstractClassAssert<ClassAssert> {
	public ClassAssert(Class<?> actual) {
		super( actual, ClassAssert.class );
	}

	public static ClassAssert assertThatClass(Class<?> actual) {
		return new ClassAssert( actual );
	}

	/// Verifies that the class declares the given fields, including synthetic fields.
	@Override
	public ClassAssert hasDeclaredFields(String... fields) {
		isNotNull();
		assertThat( actual.getDeclaredFields() )
				.as( "Declared fields of %s", actual.getName() )
				.extracting( Field::getName )
				.contains( fields );
		return this;
	}
}
