/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.persistence.FetchType;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Maps a to-many cardinality association taking values over several
 * entity types which are <em>not</em> related by the usual entity
 * inheritance, using a discriminator value stored in an
 * {@linkplain jakarta.persistence.JoinTable association table}.
 * <p>
 * The annotated property should be of type {@link java.util.List},
 * {@link java.util.Set}, {@link java.util.Collection}, or
 * {@link java.util.Map}, and the elements of the collection must be
 * entities.
 * <p>
 * For example:
 * <pre>
 * &#64;ManyToAny
 * &#64;Column(name = "property_type")
 * &#64;AnyKeyJavaClass(Long.class)
 * &#64;AnyDiscriminatorValue(discriminator = "S", entity = StringProperty.class)
 * &#64;AnyDiscriminatorValue(discriminator = "I", entity = IntegerProperty.class)
 * &#64;JoinTable(name = "repository_properties",
 *            joinColumns = @JoinColumn(name = "repository_id"),
 *            inverseJoinColumns = @JoinColumn(name = "property_id"))
 * &#64;Cascade(PERSIST)
 * private List&lt;Property&lt;?&gt;&gt; properties = new ArrayList&lt;&gt;();
 * </pre>
 * <p>
 * In this example, {@code Property} is not required to be an entity type,
 * it might even just be an interface, but its subtypes {@code StringProperty}
 * and {@code IntegerProperty} must be entity classes with the same identifier
 * type.
 * <p>
 * This is just the many-valued form of {@link Any}, and the mapping
 * options are similar, except that the
 * {@link jakarta.persistence.JoinTable @JoinTable} annotation is used
 * to specify the association table.
 * <ul>
 *     <li>{@link AnyDiscriminator}, {@link JdbcType}, or {@link JdbcTypeCode}
 *         specifies the type of the discriminator,
 *     <li>{@link AnyDiscriminatorValue} specifies how discriminator values
 *         map to entity types.
 *     <li>{@link AnyKeyJavaType}, {@link AnyKeyJavaClass}, {@link AnyKeyJdbcType},
 *         or {@link AnyKeyJdbcTypeCode} specifies the type of the foreign key.
 *     <li>{@link jakarta.persistence.JoinTable} specifies the name of the
 *         association table and its foreign key columns.
 *     <li>{@link jakarta.persistence.Column} specifies the column of the
 *         association table in which the discriminator value is stored.
 * </ul>
 *
 * @see Any
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface ManyToAny {
	/**
	 * Specifies whether the value of the field or property should be fetched
	 * lazily or eagerly:
	 * <ul>
	 * <li>{@link FetchType#EAGER}, the default, requires that the association
	 *     be fetched immediately, but
	 * <li>{@link FetchType#LAZY} is a hint which has no effect unless bytecode
	 *     enhancement is enabled.
	 * </ul>
	 */
	FetchType fetch() default FetchType.EAGER;
}
