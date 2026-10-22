package org.hibernate.internal.util;

import org.hibernate.testing.orm.junit.Jira;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.List;

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

	abstract static class AbstractBaseConverter<T> implements AttributeConverter<T, String> {
	}

	abstract static class AbstractMiddleConverter<T> extends AbstractBaseConverter<T> {
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-20662")
	public void testGenericArrayConverterTypeVariable() {
		Type[] types = GenericsHelper.typeArguments(
				AttributeConverter.class,
				StringArrayConverter.class
		);

		assertThat( types ).hasSize( 2 );
		assertThat( types[0] ).isEqualTo( String[].class );
		assertThat( types[1] ).isEqualTo( String.class );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-20662")
	public void testUnboundGenericArrayConverterTypeVariable() {
		Type[] types = GenericsHelper.typeArguments(
				AttributeConverter.class,
				AbstractArrayConverter.class
		);

		assertThat( types ).hasSize( 2 );
		assertThat( types[0] ).isEqualTo( Object[].class );
		assertThat( types[1] ).isEqualTo( String.class );
	}

	abstract static class AbstractArrayConverter<T> implements AttributeConverter<T[], String> {
	}

	abstract static class StringArrayConverter extends AbstractArrayConverter<String> {
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-20662")
	public void testWildcardConverter() {
		Type[] types = GenericsHelper.typeArguments(
				AttributeConverter.class,
				IntegerWildcardConverter.class
		);

		assertThat( types ).hasSize( 2 );

		// expected: List<? extends Integer>
		WildcardType wildcard = listWildcard( types[0] );
		assertThat( wildcard.getUpperBounds() ).containsExactly( Integer.class );
		assertThat( wildcard.getLowerBounds() ).isEmpty();

		assertThat( types[1] ).isEqualTo( String.class );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-20662")
	public void testSuperWildcardConverter() {
		Type[] types = GenericsHelper.typeArguments(
				AttributeConverter.class,
				IntegerSuperWildcardConverter.class
		);

		assertThat( types ).hasSize( 2 );

		// expected: List<? super Integer>
		WildcardType wildcard = listWildcard( types[0] );
		assertThat( wildcard.getUpperBounds() ).containsExactly( Object.class );
		assertThat( wildcard.getLowerBounds() ).containsExactly( Integer.class );

		assertThat( types[1] ).isEqualTo( String.class );
	}

	private static WildcardType listWildcard(Type type) {
		assertThat( type ).isInstanceOf( ParameterizedType.class );
		ParameterizedType list = (ParameterizedType) type;
		assertThat( list.getRawType() ).isEqualTo( List.class );
		assertThat( list.getActualTypeArguments() ).hasSize( 1 );
		assertThat( list.getActualTypeArguments()[0] ).isInstanceOf( WildcardType.class );
		return (WildcardType) list.getActualTypeArguments()[0];
	}

	abstract static class WildcardConverter<T> implements AttributeConverter<List<? extends T>, String> {
	}

	abstract static class IntegerWildcardConverter extends WildcardConverter<Integer> {
	}

	abstract static class SuperWildcardConverter<T> implements AttributeConverter<List<? super T>, String> {
	}

	abstract static class IntegerSuperWildcardConverter extends SuperWildcardConverter<Integer> {
	}
}
