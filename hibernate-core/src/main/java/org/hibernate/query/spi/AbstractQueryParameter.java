package org.hibernate.query.spi;


import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.Internal;
import org.hibernate.type.BindableType;

import static org.hibernate.query.internal.QueryLogging.QUERY_MESSAGE_LOGGER;

/**
 * Base implementation of {@link org.hibernate.query.QueryParameter}.
 *
 * @apiNote This class is now considered internal implementation
 * and will move to an internal package in a future version.
 * Application programs should never depend directly on this class.
 *
 * @author Steve Ebersole
 */
@Internal
public abstract class AbstractQueryParameter<T> implements QueryParameterImplementor<T> {

	private boolean allowMultiValuedBinding;
	private @Nullable BindableType<T> anticipatedType;
	// A Criteria declaration may have a Java type before its binding type is inferred.
	protected final @Nullable Class<T> declaredJavaType;

	public AbstractQueryParameter(boolean allowMultiValuedBinding, @Nullable BindableType<T> anticipatedType) {
		this( allowMultiValuedBinding, anticipatedType, null );
	}

	protected AbstractQueryParameter(
			boolean allowMultiValuedBinding,
			@Nullable BindableType<T> anticipatedType,
			@Nullable Class<T> declaredJavaType) {
		this.allowMultiValuedBinding = allowMultiValuedBinding;
		this.anticipatedType = anticipatedType;
		this.declaredJavaType = declaredJavaType;
	}

	@Override
	public void disallowMultiValuedBinding() {
		QUERY_MESSAGE_LOGGER.debugf( "QueryParameter#disallowMultiValuedBinding() called: %s", this );
		this.allowMultiValuedBinding = false;
	}

	@Override
	public boolean allowsMultiValuedBinding() {
		return allowMultiValuedBinding;
	}

	@Override
	public @Nullable BindableType<T> getHibernateType() {
		return anticipatedType;
	}

	@Override
	public void applyAnticipatedType(@Nullable BindableType<?> type) {
		//noinspection unchecked
		this.anticipatedType = (BindableType<T>) type;
	}

	@Override
	@Nullable
	public String getName() {
		return null;
	}

	@Override
	@Nullable
	public Integer getPosition() {
		return null;
	}

	@Override
	@Nonnull
	public Class<T> getParameterType() {
		final var javaType = getParameterTypeIfKnown();
		if ( javaType != null ) {
			return javaType;
		}
		throw new IllegalStateException( "Could not determine the Java type of query parameter "
				+ (getName() != null ? ":" + getName() : "?" + getPosition()) );
	}

	/**
	 * Internal access to the Java type while parameter type inference is incomplete.
	 */
	public @Nullable Class<T> getParameterTypeIfKnown() {
		if ( declaredJavaType != null ) {
			return declaredJavaType;
		}
		return anticipatedType == null ? null : anticipatedType.getJavaType();
	}
}
