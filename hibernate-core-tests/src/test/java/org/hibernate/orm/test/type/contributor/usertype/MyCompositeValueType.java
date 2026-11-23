package org.hibernate.orm.test.type.contributor.usertype;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;

public class MyCompositeValueType implements CompositeUserType<MyCompositeValue> {
	public static final MyCompositeValueType INSTANCE = new MyCompositeValueType();

	public static class EmbeddableMapper {
		Long longValue;
		String stringValue;
	}

	@Override
	@Nullable
	public Object getPropertyValue(@Nonnull MyCompositeValue component, int property) throws HibernateException {
		return switch ( property ) {
			case 0 -> component.longValue();
			case 1 -> component.stringValue();
			default -> null;
		};
	}

	@Override
	@Nonnull
	public MyCompositeValue instantiate(@Nonnull ValueAccess values) {
		final Long id = values.getValue( 0, Long.class );
		final String hash = values.getValue( 1, String.class );
		return new MyCompositeValue( id, hash );
	}

	@Override
	@Nonnull
	public Class<?> embeddable() {
		return EmbeddableMapper.class;
	}

	@Override
	@Nonnull
	public Class<MyCompositeValue> returnedClass() {
		return MyCompositeValue.class;
	}

	@Override
	public boolean equals(@Nullable MyCompositeValue x, @Nullable MyCompositeValue y) {
		return Objects.equals( x, y );
	}

	@Override
	public int hashCode(@Nonnull MyCompositeValue x) {
		return Objects.hashCode( x );
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	@Nonnull
	public MyCompositeValue deepCopy(@Nonnull MyCompositeValue value) {
		return new MyCompositeValue( value.longValue(), value.stringValue() );
	}

	@Override
	@Nonnull
	public Serializable disassemble(@Nonnull MyCompositeValue value) {
		return new Object[] { value.longValue(), value.stringValue() };
	}

	@Override
	@Nonnull
	public MyCompositeValue assemble(@Nonnull Serializable cached, @Nullable Object owner) {
		final Object[] parts = (Object[]) cached;
		return new MyCompositeValue( (Long) parts[0], (String) parts[1] );
	}

	@Override
	@Nonnull
	public MyCompositeValue replace(
			@Nonnull MyCompositeValue detached,
			@Nullable MyCompositeValue managed,
			@Nullable Object owner) {
		return deepCopy( detached );
	}
}
