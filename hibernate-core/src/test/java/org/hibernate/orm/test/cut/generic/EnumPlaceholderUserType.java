package org.hibernate.orm.test.cut.generic;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;

public class EnumPlaceholderUserType implements CompositeUserType<EnumPlaceholder> {

	@Override
	@Nonnull
	public Object getPropertyValue(@Nonnull EnumPlaceholder component, int property) throws HibernateException {
		return switch ( property ) {
			case 0 -> component.getFirstEnum().getClass();
			case 1 -> component.getFirstEnum().name();
			case 2 -> component.getSecondEnum().getClass();
			case 3 -> component.getSecondEnum().name();
			default -> throw new RuntimeException();
		};
	}

	@Override
	@Nonnull
	public EnumPlaceholder instantiate(@Nonnull ValueAccess values) {
		Class<? extends Enum> firstEnumClass = values.getValue( 0, Class.class );
		String firstEnumValue = values.getValue( 1, String.class );
		Class<? extends Enum> secondEnumClass = values.getValue( 2, Class.class );
		String secondEnumValue = values.getValue( 3, String.class );

		Enum firstEnum = Enum.valueOf( firstEnumClass, firstEnumValue );
		Enum secondEnum = Enum.valueOf( secondEnumClass, secondEnumValue );

		return new EnumPlaceholder( firstEnum, secondEnum );
	}

	@Override
	@Nonnull
	public Class<?> embeddable() {
		return EmbeddableMapper.class;
	}

	@Override
	@Nonnull
	public Class<EnumPlaceholder> returnedClass() {
		return EnumPlaceholder.class;
	}

	@Override
	public boolean equals(@Nullable EnumPlaceholder x, @Nullable EnumPlaceholder y) {
		return Objects.equals( x, y );
	}

	@Override
	public int hashCode(@Nonnull EnumPlaceholder x) {
		return Objects.hashCode( x );
	}

	@Override
	@Nullable
	public EnumPlaceholder deepCopy(@Nullable EnumPlaceholder value) {
		return value == null ? null : new EnumPlaceholder<>( value.getFirstEnum(), value.getSecondEnum() );
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	@Nullable
	public Serializable disassemble(@Nullable EnumPlaceholder value) {
		return (Serializable) value;
	}

	@Override
	@Nullable
	public EnumPlaceholder assemble(@Nullable Serializable cached, @Nullable Object owner) {
		return (EnumPlaceholder) cached;
	}

	@Override
	@Nullable
	public EnumPlaceholder replace(
			@Nullable EnumPlaceholder detached,
			@Nullable EnumPlaceholder managed,
			@Nullable Object owner) {
		return detached;
	}

	public static class EmbeddableMapper {
		Class<? extends Enum> firstEnumClass;

		String firstEnumValue;

		Class<? extends Enum> secondEnumClass;

		String secondEnumValue;
	}

}
