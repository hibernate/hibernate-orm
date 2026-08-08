/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

import org.hibernate.testing.orm.junit.Jira;
import org.junit.jupiter.api.Test;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import jakarta.persistence.*;

import static org.assertj.core.api.Assertions.assertThat;

public class GenericsHelperTest {
	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-20265")
	public void testChainedTypeVariable() throws Exception {
		Type t = GenericsHelper.actualInheritedMemberType(
				BaseLibraryEntity.class,
				BaseLibraryEntity.class.getDeclaredField( "processed" )
		);

		assertThat(t).isEqualTo(boolean.class);
	}

	@MappedSuperclass
	static abstract class BaseEntity<T> {
	}

	@MappedSuperclass
	static abstract class BaseReadableEntity<T> extends BaseEntity<T> {
	}

	@MappedSuperclass
	static abstract class BaseLibraryEntity<T> extends BaseReadableEntity<T> {
		private boolean processed;
	}

	@Entity
	static class Book extends BaseLibraryEntity<String> {
		@Id
		private String id;
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-20467")
	public void testUnboundConverterTypeVariable() {
		Type[] types = GenericsHelper.typeArguments(
				AttributeConverter.class,
				AbstractMiddleConverter.class
		);

		assertThat( types ).hasSize( 2 );
		assertThat( types[0] ).isEqualTo( Object.class );
		assertThat( types[1] ).isEqualTo( String.class );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-20662")
	void testTypeArgumentsWithGenericArrayType()   {
		Type type = GenericsHelper.typeArguments(
				AttributeConverter.class,
				StringArrayConverter.class
		)[0];
		assertThat( type ).isInstanceOf( GenericArrayType.class );
		assertThat( ((GenericArrayType) type ).getGenericComponentType() ).isEqualTo( String.class );
	}

	abstract static class AbstractBaseConverter<T> implements AttributeConverter<T, String> {
	}

	abstract static class AbstractMiddleConverter<T> extends AbstractBaseConverter<T> {
	}

	static abstract class AbstractArrayConverter<T> implements AttributeConverter<T[], String> {

	}

	static class StringArrayConverter extends AbstractArrayConverter<String> {

		@Override
		public String convertToDatabaseColumn(String[] attribute) {
			return "";
		}

		@Override
		public String[] convertToEntityAttribute(String dbData) {
			return new String[0];
		}
	}

}
