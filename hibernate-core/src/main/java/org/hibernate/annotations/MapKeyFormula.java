package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies an expression written in native SQL that is used to read the key
 * of a {@link java.util.Map} instead of storing it in a
 * {@link jakarta.persistence.MapKeyColumn}.
 * <p>
 * The expression is evaluated against the table holding the map entries, that is,
 * the collection table, or the table of the target entity for a one-to-many
 * association without a join table:
 * <pre>
 * &#64;OneToMany(mappedBy = "parent")
 * &#64;MapKeyFormula("upper(code)")
 * Map&lt;String, Child&gt; children;
 * </pre>
 * <p>
 * Since the key is derived, it is never written to the database.
 *
 * @see Formula
 * @see jakarta.persistence.MapKeyColumn
 *
 * @since 8.1
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface MapKeyFormula {
	/**
	 * The formula, written in native SQL.
	 */
	String value();
}
