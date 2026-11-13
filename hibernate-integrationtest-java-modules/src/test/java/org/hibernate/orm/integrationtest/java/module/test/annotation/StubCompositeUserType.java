package org.hibernate.orm.integrationtest.java.module.test.annotation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;

import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;

public class StubCompositeUserType implements CompositeUserType<StubCompositeType> {
	@Override
	@Nullable
	public Object getPropertyValue(@Nonnull StubCompositeType component, int property) {
		return component.value;
	}

	@Override
	@Nonnull
	public StubCompositeType instantiate(@Nonnull ValueAccess values) {
		return new StubCompositeType( values.getValue( 0, String.class ) );
	}

	@Override
	@Nonnull
	public Class<?> embeddable() {
		return StubEmbeddable.class;
	}

	@Override
	@Nonnull
	public Class<StubCompositeType> returnedClass() {
		return StubCompositeType.class;
	}

	@Override
	public boolean equals(@Nullable StubCompositeType x, @Nullable StubCompositeType y) {
		return x == y || ( x != null && y != null && x.value.equals( y.value ) );
	}

	@Override
	public int hashCode(@Nonnull StubCompositeType x) {
		return x == null ? 0 : x.value.hashCode();
	}

	@Override
	@Nonnull
	public StubCompositeType deepCopy(@Nonnull StubCompositeType value) {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	@Nonnull
	public Serializable disassemble(@Nonnull StubCompositeType value) {
		return new String[] { value.value };
	}

	@Override
	@Nonnull
	public StubCompositeType assemble(@Nonnull Serializable cached, @Nullable Object owner) {
		return new StubCompositeType( ( (String[]) cached )[0] );
	}

	@Override
	@Nonnull
	public StubCompositeType replace(
			@Nonnull StubCompositeType detached,
			@Nullable StubCompositeType managed,
			@Nullable Object owner) {
		return detached;
	}
}
