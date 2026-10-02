package org.hibernate.query.range;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.hibernate.Incubating;
import org.hibernate.Internal;

import java.util.List;

/**
 * Specifies an allowed set or range of values for a value being restricted.
 * <p>
 * A parameter of a {@linkplain org.hibernate.annotations.processing.Find
 * finder method} may be declared with type {@code Range<T>} where {@code T}
 * is the type of the matching field or property of the entity.
 *
 * @param <U> The type of the value being restricted
 *
 * @see org.hibernate.query.restriction.Restriction
 * @see org.hibernate.query.restriction.Restriction#restrict(jakarta.persistence.metamodel.SingularAttribute, Range)
 *
 * @author Gavin King
 *
 * @since 7.0
 */
@Incubating(since = "7.0", group = "query-specifications")
public interface Range<U> {

	/**
	 * The Java class of the values belonging to this range.
	 */
	@Nonnull
	Class<? extends U> getType();

	/**
	 * Return a JPA Criteria {@link Predicate} constraining the given
	 * attribute of the given root entity to this domain of allowed
	 * values.
	 */
	@Internal
	@Nonnull
	Predicate toPredicate(@Nonnull Path<? extends U> path, @Nonnull CriteriaBuilder builder);

	/**
	 * A range containing a single value.
	 */
	@Nonnull
	static <U> Range<U> singleValue(@Nonnull U value) {
		return new Value<>( value );
	}

	/**
	 * A range containing all strings which are equal to the given string,
	 * ignoring case.
	 */
	@Nonnull
	static Range<String> singleCaseInsensitiveValue(@Nonnull String value) {
		return new CaseInsensitiveValue( value );
	}

	/**
	 * A range containing all values belonging to the given list.
	 */
	@Nonnull
	static <U> Range<U> valueList(@Nonnull List<U> values) {
		return new ValueList<>( values );
	}

	/**
	 * A range containing all values strictly greater than the given
	 * lower bound.
	 */
	@Nonnull
	static <U extends Comparable<U>> Range<U> greaterThan(@Nonnull U bound) {
		return new LowerBound<>( bound, true );
	}

	/**
	 * A range containing all values greater than or equal to the given
	 * lower bound.
	 */
	@Nonnull
	static <U extends Comparable<U>> Range<U> greaterThanOrEqualTo(@Nonnull U bound) {
		return new LowerBound<>( bound, false );
	}

	/**
	 * A range containing all values strictly less than the given
	 * upper bound.
	 */
	@Nonnull
	static <U extends Comparable<U>> Range<U> lessThan(@Nonnull U bound) {
		return new UpperBound<>( bound, true );
	}

	/**
	 * A range containing all values less than or equal to the given
	 * upper bound.
	 */
	@Nonnull
	static <U extends Comparable<U>> Range<U> lessThanOrEqualTo(@Nonnull U bound) {
		return new UpperBound<>( bound, false );
	}

	/**
	 * An open range containing all values strictly greater than the
	 * given lower bound, and strictly less than the given upper bound.
	 */
	@Nonnull
	static <U extends Comparable<U>> Range<U> open(@Nonnull U lowerBound, @Nonnull U upperBound) {
		return new Interval<>( new LowerBound<>( lowerBound, true ),
				new UpperBound<>( upperBound, true ) );
	}

	/**
	 * A closed range containing all values greater than or equal to the
	 * given lower bound, and less than or equal to the given upper bound.
	 */
	@Nonnull
	static <U extends Comparable<U>> Range<U> closed(@Nonnull U lowerBound, @Nonnull U upperBound) {
		return new Interval<>( new LowerBound<>( lowerBound, false ),
				new UpperBound<>( upperBound, false ) );
	}

	/**
	 * A range containing all strings which match the given pattern,
	 * with case-sensitivity specified explicitly. The pattern must
	 * be expressed in terms of the default wildcard characters
	 * {@code _} and {@code %}.
	 *
	 * @param pattern A pattern involving the default wildcard characters
	 * @param caseSensitive {@code true} if matching is case-sensitive
	 */
	@Nonnull
	static Range<String> pattern(@Nonnull String pattern, boolean caseSensitive) {
		return new Pattern( pattern, caseSensitive );
	}

	/**
	 * A range containing all strings which match the given pattern,
	 * with case-sensitivity specified explicitly. The pattern must
	 * be expressed in terms of the given single-character and
	 * multi-character wildcards.
	 *
	 * @param pattern A pattern involving the given wildcard characters
	 * @param caseSensitive {@code true} if matching is case-sensitive
	 * @param charWildcard A wildcard character which matches any single character
	 * @param stringWildcard A wildcard character which matches any string of characters
	 */
	@Nonnull
	static Range<String> pattern(@Nonnull String pattern, boolean caseSensitive, char charWildcard, char stringWildcard) {
		return new Pattern( pattern, caseSensitive, charWildcard, stringWildcard );
	}

	/**
	 * A range containing all strings which match the given pattern,
	 * with case-sensitivity. The pattern must be expressed in terms
	 * of the default wildcard characters {@code _} and {@code %}.
	 */
	@Nonnull
	static Range<String> pattern(@Nonnull String pattern) {
		return pattern( pattern, true );
	}

	/**
	 * A range containing all strings which begin with the given prefix,
	 * with case-sensitivity specified explicitly.
	 */
	@Nonnull
	static Range<String> prefix(@Nonnull String prefix, boolean caseSensitive) {
		return pattern( escape( prefix ) + '%', caseSensitive );
	}

	/**
	 * A range containing all strings which end with the given suffix,
	 * with case-sensitivity specified explicitly.
	 */
	@Nonnull
	static Range<String> suffix(@Nonnull String suffix, boolean caseSensitive) {
		return pattern( '%' + escape( suffix ), caseSensitive );
	}

	/**
	 * A range containing all strings which contain the given substring,
	 * with case-sensitivity specified explicitly.
	 */
	@Nonnull
	static Range<String> containing(@Nonnull String substring, boolean caseSensitive) {
		return pattern( '%' + escape( substring ) + '%', caseSensitive );
	}

	/**
	 * A range containing all strings which begin with the given prefix,
	 * with case-sensitivity.
	 */
	@Nonnull
	static Range<String> prefix(@Nonnull String prefix) {
		return prefix( prefix, true );
	}

	/**
	 * A range containing all strings which end with the given suffix,
	 * with case-sensitivity.
	 */
	@Nonnull
	static Range<String> suffix(@Nonnull String suffix) {
		return suffix( suffix, true );
	}

	/**
	 * A range containing all strings which contain the given substring,
	 * with case-sensitivity.
	 */
	@Nonnull
	static Range<String> containing(@Nonnull String substring) {
		return containing( substring, true );
	}

	/**
	 * An empty range containing no values.
	 */
	@Nonnull
	static <U> Range<U> empty(@Nonnull Class<U> type) {
		return new EmptyRange<>( type );
	}

	/**
	 * A complete range containing all values of the given type.
	 */
	@Nonnull
	static <U> Range<U> full(@Nonnull Class<U> type) {
		return new FullRange<>( type );
	}

	/**
	 * A range containing all allowed values of the given type
	 * except {@code null}.
	 */
	@Nonnull
	static <U> Range<U> notNull(@Nonnull Class<U> type) {
		return new NotNull<>( type );
	}

	/**
	 * Escape occurrences of the default wildcard characters in the
	 * given literal string.
	 */
	@Nonnull
	private static String escape(@Nonnull String literal) {
		final var result = new StringBuilder();
		for ( int i = 0; i < literal.length(); i++ ) {
			final char ch = literal.charAt( i );
			if ( ch=='%' || ch=='_' || ch=='\\' ) {
				result.append('\\');
			}
			result.append( ch );
		}
		return result.toString();
	}
}
