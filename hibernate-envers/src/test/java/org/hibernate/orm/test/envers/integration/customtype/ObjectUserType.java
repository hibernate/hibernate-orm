package org.hibernate.orm.test.envers.integration.customtype;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;

import jakarta.persistence.Lob;

/**
 * Custom type used to persist binary representation of Java object in the database.
 * Spans over two columns - one storing text representation of Java class name and the second one
 * containing binary data.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class ObjectUserType implements CompositeUserType<Object> {

	@Override
	@Nullable
	public Object getPropertyValue(@Nonnull Object component, int property) throws HibernateException {
		return switch ( property ) {
			case 0 -> component;
			case 1 -> component.getClass().getName();
			default -> null;
		};
	}

	@Override
	@Nullable
	public Object instantiate(@Nonnull ValueAccess valueAccess) {
		return valueAccess.getValue( 0, Object.class );
	}

	@Override
	@Nonnull
	public Class<?> embeddable() {
		return TaggedObject.class;
	}

	@Override
	@Nonnull
	public Class<Object> returnedClass() {
		return Object.class;
	}

	@Override
	public boolean equals(@Nullable Object x, @Nullable Object y) throws HibernateException {
		if ( x == y ) {
			return true;
		}
		if ( x == null || y == null ) {
			return false;
		}
		return x.equals( y );
	}

	@Override
	public int hashCode(@Nonnull Object x) throws HibernateException {
		return x.hashCode();
	}

	@Override
	@Nullable
	public Object deepCopy(@Nullable Object value) throws HibernateException {
		return value; // Persisting only immutable types.
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	@Nullable
	public Serializable disassemble(@Nullable Object value) throws HibernateException {
		return (Serializable) value;
	}

	@Override
	@Nullable
	public Object assemble(@Nullable Serializable cached, @Nullable Object owner) throws HibernateException {
		return cached;
	}

	@Override
	@Nullable
	public Object replace(
			@Nullable Object original,
			@Nullable Object target,
			@Nullable Object owner) throws HibernateException {
		return original;
	}

	public static class TaggedObject {
		String type;
		@Lob
		Serializable object;
	}
}
