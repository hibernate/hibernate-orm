package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.metamodel.BooleanAttribute;
import jakarta.persistence.metamodel.ComparableAttribute;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.NumericAttribute;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.TemporalAttribute;
import jakarta.persistence.metamodel.TextAttribute;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.spi.NavigablePath;

import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Map;

/**
 * API extension to the JPA {@link Path} contract
 *
 * @author Steve Ebersole
 */
public interface JpaPath<T> extends JpaExpression<T>, Path<T> {
	/**
	 * Get this path's NavigablePath
	 */
	@Nonnull
	NavigablePath getNavigablePath();

	/**
	 * The source (think "left hand side") of this path
	 */
	@Nullable
	JpaPath<?> getLhs();

	/**
	 * Support for JPA's explicit (TREAT) down-casting.
	 */
	@Nonnull
	<S extends T> JpaTreatedPath<T,S> treatAs(@Nonnull Class<S> treatJavaType);

	/**
	 * Downcast this path to the specified subtype.
	 */
	@Override
	@Nonnull
	default <S extends T> JpaPath<S> treat(@Nonnull Class<S> treatJavaType) {
		return treatAs( treatJavaType );
	}

	/**
	 * Support for JPA's explicit (TREAT) down-casting.
	 */
	@Nonnull
	<S extends T> JpaPath<S> treatAs(@Nonnull EntityDomainType<S> treatJavaType);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Covariant overrides

	/**
	 * Return the parent path.
	 */
	@Nullable
	@Override
	default JpaPath<?> getParentPath() {
		return getLhs();
	}

	/**
	 * Create a path for the given attribute.
	 */
	@Nonnull
	@Override
	<Y> JpaPath<Y> get(@Nonnull SingularAttribute<? super T, Y> attribute);

	/**
	 * Create a numeric expression for the given numeric attribute.
	 * Overrides the JPA standard method to return Hibernate-specific type.
	 */
	@Nonnull
	@Override
	<N extends Number & Comparable<N>> JpaNumericExpression<N> get(@Nonnull NumericAttribute<? super T, N> attribute);

	/**
	 * Create a text expression for the given text attribute.
	 * Overrides the JPA standard method to return Hibernate-specific type.
	 */
	@Nonnull
	@Override
	JpaTextExpression get(@Nonnull TextAttribute<? super T> attribute);

	/**
	 * Create a boolean expression for the given boolean attribute.
	 * Overrides the JPA standard method to return Hibernate-specific type.
	 */
	@Nonnull
	@Override
	JpaBooleanExpression get(@Nonnull BooleanAttribute<? super T> attribute);

	/**
	 * Create a temporal expression for the given temporal attribute.
	 * Overrides the JPA standard method to return Hibernate-specific type.
	 */
	@Nonnull
	@Override
	<T1 extends Temporal & Comparable<? super T1>> JpaTemporalExpression<T1> get(@Nonnull TemporalAttribute<? super T, T1> attribute);

	/**
	 * Create a comparable expression for the given comparable attribute.
	 * Overrides the JPA standard method to return Hibernate-specific type.
	 */
	@Nonnull
	@Override
	<C extends Comparable<? super C>> JpaComparableExpression<C> get(@Nonnull ComparableAttribute<? super T, C> attribute);

	/**
	 * Create a path for the given attribute.
	 */
	@Nonnull
	@Override
	<E, C extends Collection<E>> JpaPluralExpression<C,E> get(@Nonnull PluralAttribute<? super T, C, E> collection);

	/**
	 * Create a path for the given attribute.
	 */
	@Nonnull
	@Override
	<K, V, M extends Map<K, V>> JpaPluralExpression<M,V> get(@Nonnull MapAttribute<? super T, K, V> map);

	/**
	 * Create an expression for the type of this path.
	 */
	@Nonnull
	@Override
	JpaExpression<Class<? extends T>> type();

	/**
	 * Create a path for the given attribute.
	 */
	@Nonnull
	@Override
	<Y> JpaPath<Y> get(@Nonnull String attributeName);
}
