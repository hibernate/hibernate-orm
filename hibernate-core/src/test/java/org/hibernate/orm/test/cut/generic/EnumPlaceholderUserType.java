/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cut.generic;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;

public class EnumPlaceholderUserType implements CompositeUserType<EnumPlaceholder> {

	@Override
	public Object getPropertyValue(EnumPlaceholder component, int property) throws HibernateException {
		return switch ( property ) {
			case 0 -> component.getFirstEnum().getClass();
			case 1 -> component.getFirstEnum().name();
			case 2 -> component.getSecondEnum().getClass();
			case 3 -> component.getSecondEnum().name();
			default -> throw new RuntimeException();
		};
	}

	@Override
	public EnumPlaceholder instantiate(ValueAccess values) {
		Class<? extends Enum> firstEnumClass = values.getValue( 0, Class.class );
		String firstEnumValue = values.getValue( 1, String.class );
		Class<? extends Enum> secondEnumClass = values.getValue( 2, Class.class );
		String secondEnumValue = values.getValue( 3, String.class );

		Enum firstEnum = Enum.valueOf( firstEnumClass, firstEnumValue );
		Enum secondEnum = Enum.valueOf( secondEnumClass, secondEnumValue );

		return new EnumPlaceholder( firstEnum, secondEnum );
	}

	@Override
	public Class<?> embeddable() {
		return EmbeddableMapper.class;
	}

	@Override
	public Class<EnumPlaceholder> returnedClass() {
		return EnumPlaceholder.class;
	}

	@Override
	public boolean equals(EnumPlaceholder x, EnumPlaceholder y) {
		return Objects.equals( x, y );
	}

	@Override
	public int hashCode(EnumPlaceholder x) {
		return Objects.hashCode( x );
	}

	@Override
	public EnumPlaceholder deepCopy(EnumPlaceholder value) {
		return new EnumPlaceholder<>( value.getFirstEnum(), value.getSecondEnum() );
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(EnumPlaceholder value) {
		return (Serializable) value;
	}

	@Override
	public EnumPlaceholder assemble(Serializable cached, Object owner) {
		return (EnumPlaceholder) cached;
	}

	@Override
	public EnumPlaceholder replace(EnumPlaceholder detached, EnumPlaceholder managed, Object owner) {
		return detached;
	}

	public static class EmbeddableMapper {
		Class<? extends Enum> firstEnumClass;

		String firstEnumValue;

		Class<? extends Enum> secondEnumClass;

		String secondEnumValue;
	}

}
