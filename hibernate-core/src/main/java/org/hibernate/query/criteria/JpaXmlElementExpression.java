package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;

import java.util.List;

import org.hibernate.Incubating;

import jakarta.persistence.criteria.Expression;

/**
 * A special expression for the {@code xmlelement} function.
 * @since 7.0
 */
@Incubating(since = "6.3")
public interface JpaXmlElementExpression extends JpaExpression<String> {

	/**
	 * Passes the given {@link Expression} as value for the XML attribute with the given name.
	 *
	 * @return {@code this} for method chaining
	 */
	@Nonnull
	JpaXmlElementExpression attribute(@Nonnull String attributeName, @Nonnull Expression<?> expression);

	/**
	 * Passes the given {@link Expression}s as value for the XML content of this element.
	 *
	 * @return {@code this} for method chaining
	 */
	@Nonnull
	JpaXmlElementExpression content(@Nonnull List<? extends Expression<?>> expressions);

	/**
	 * Passes the given {@link Expression}s as value for the XML content of this element.
	 *
	 * @return {@code this} for method chaining
	 */
	@Nonnull
	JpaXmlElementExpression content(@Nonnull Expression<?>... expressions);
}
