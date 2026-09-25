package org.hibernate.query.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.query.QueryParameter;
import org.hibernate.query.named.spi.NamedQueryMemento;
import org.hibernate.type.BindableType;

/**
 * @author Steve Ebersole
 */
public interface QueryParameterImplementor<T> extends QueryParameter<T> {
	void disallowMultiValuedBinding();

	void applyAnticipatedType(@Nullable BindableType<?> type);

	@Nonnull
	NamedQueryMemento.ParameterMemento toMemento();
}
