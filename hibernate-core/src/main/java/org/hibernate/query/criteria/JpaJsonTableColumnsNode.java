package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Incubating;

/**
 * A special expression for the definition of columns within the {@code json_table} function.
 * @since 7.0
 */
@Incubating(since = "6.3")
public interface JpaJsonTableColumnsNode {

	/**
	 * Like {@link #existsColumn(String, String)}, but uses the column name as JSON path expression.
	 *
	 * @return The {@link JpaJsonExistsNode} for the column
	 */
	@Nonnull
	JpaJsonExistsNode existsColumn(@Nonnull String columnName);

	/**
	 * Defines a boolean column on the result type with the given name for which the value can be obtained
	 * by invoking {@code json_exists} with the given JSON path.
	 *
	 * @return The {@link JpaJsonExistsNode} for the column
	 */
	@Nonnull
	JpaJsonExistsNode existsColumn(@Nonnull String columnName, @Nullable String jsonPath);

	/**
	 * Like {@link #queryColumn(String, String)}, but uses the column name as JSON path expression.
	 *
	 * @return The {@link JpaJsonQueryNode} for the column
	 */
	@Nonnull
	JpaJsonQueryNode queryColumn(@Nonnull String columnName);

	/**
	 * Defines a string column on the result type with the given name for which the value can be obtained
	 * by invoking {@code json_query} with the given JSON path.
	 *
	 * @return The {@link JpaJsonQueryNode} for the column
	 */
	@Nonnull
	JpaJsonQueryNode queryColumn(@Nonnull String columnName, @Nullable String jsonPath);

	/**
	 * Like {@link #valueColumn(String, Class, String)} but uses the column name as JSON path expression.
	 *
	 * @return The {@link JpaJsonValueNode} for the column
	 */
	@Nonnull
	<T> JpaJsonValueNode<T> valueColumn(@Nonnull String columnName, @Nonnull Class<T> type);

	/**
	 * Defines a column on the result type with the given name and type for which the value can be obtained by the given JSON path expression.
	 *
	 * @return The {@link JpaJsonValueNode} for the column
	 */
	@Nonnull
	<T> JpaJsonValueNode<T> valueColumn(@Nonnull String columnName, @Nonnull Class<T> type, @Nullable String jsonPath);

	/**
	 * Like {@link #valueColumn(String, Class, String)} but uses the column name as JSON path expression.
	 *
	 * @return The {@link JpaJsonValueNode} for the column
	 */
	@Nonnull
	<T> JpaJsonValueNode<T> valueColumn(@Nonnull String columnName, @Nonnull JpaCastTarget<T> type);

	/**
	 * Defines a column on the result type with the given name and type for which the value can be obtained by the given JSON path expression.
	 *
	 * @return The {@link JpaJsonValueNode} for the column
	 */
	@Nonnull
	<T> JpaJsonValueNode<T> valueColumn(@Nonnull String columnName, @Nonnull JpaCastTarget<T> type, @Nullable String jsonPath);

	/**
	 * Defines nested columns that are accessible by the given JSON path.
	 *
	 * @return a new columns node for the nested JSON path
	 */
	@Nonnull
	JpaJsonTableColumnsNode nested(@Nonnull String jsonPath);

	/**
	 * Defines a long typed column on the result type with the given name which is set to the ordinality i.e.
	 * the 1-based position of the processed element. Ordinality starts again at 1 within nested paths.
	 *
	 * @return {@code this} for method chaining
	 */
	@Nonnull
	JpaJsonTableColumnsNode ordinalityColumn(@Nonnull String columnName);
}
