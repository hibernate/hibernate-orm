package org.hibernate.orm.test.mapping.embeddable.strategy.usertype.embedded;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;

//tag::embeddable-usertype-impl[]
public class NameCompositeUserType implements CompositeUserType<Name> {

	public static class NameMapper {
		String firstName;
		String lastName;
	}

	@Override
	@Nonnull
	public Class<?> embeddable() {
		return NameMapper.class;
	}

	@Override
	@Nonnull
	public Class<Name> returnedClass() {
		return Name.class;
	}

	@Override
	@Nonnull
	public Name instantiate(@Nonnull ValueAccess valueAccess) {
		// alphabetical
		final String first = valueAccess.getValue( 0, String.class );
		final String last = valueAccess.getValue( 1, String.class );
		return new Name( first, last );
	}

	@Override
	@Nullable
	public Object getPropertyValue(@Nonnull Name component, int property) throws HibernateException {
		// alphabetical
		switch ( property ) {
			case 0:
				return component.firstName();
			case 1:
				return component.lastName();
		}
		return null;
	}

	@Override
	public boolean equals(@Nullable Name x, @Nullable Name y) {
		return x == y || x != null && y != null && Objects.equals( x.firstName(), y.firstName() )
				&& Objects.equals( x.lastName(), y.lastName() );
	}

	@Override
	public int hashCode(@Nonnull Name x) {
		return Objects.hash( x.firstName(), x.lastName() );
	}

	@Override
	@Nullable
	public Name deepCopy(@Nullable Name value) {
		return value; // immutable
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	@Nullable
	public Serializable disassemble(@Nullable Name value) {
		return value == null ? null : new String[] { value.firstName(), value.lastName() };
	}

	@Override
	@Nullable
	public Name assemble(@Nullable Serializable cached, @Nullable Object owner) {
		if ( cached == null ) {
			return null;
		}
		final String[] parts = (String[]) cached;
		return new Name( parts[0], parts[1] );
	}

	@Override
	@Nullable
	public Name replace(@Nullable Name detached, @Nullable Name managed, @Nullable Object owner) {
		return detached;
	}

}
//end::embeddable-usertype-impl[]
