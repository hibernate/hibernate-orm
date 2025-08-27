/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies that a field or property of an entity class is part of
 * the natural id of the entity. This annotation is very useful when
 * the primary key of an entity class is a surrogate key, that is,
 * a {@linkplain jakarta.persistence.GeneratedValue system-generated}
 * synthetic identifier, with no domain-model semantics. There should
 * always be some other field or combination of fields which uniquely
 * identifies an instance of the entity from the point of view of the
 * user of the system. This is the <em>natural id</em> of the entity.
 * <p>
 * A natural id may be a single field or property of the entity:
 * <pre>
 * &#64;Entity
 * &#64;Cache &#64;NaturalIdCache
 * class Person {
 *
 *     //synthetic id
 *     &#64;GeneratedValue @Id
 *     Long id;
 *
 *     &#64;NotNull
 *     String name;
 *
 *     //simple natural id
 *     &#64;NotNull @NaturalId
 *     String ssn;
 *
 *     ...
 * }
 * </pre>
 * <p>
 * or it may be a composite value:
 * <pre>
 * &#64;Entity
 * &#64;Cache &#64;NaturalIdCache
 * class Vehicle {
 *
 *     //synthetic id
 *     &#64;GeneratedValue @Id
 *     Long id;
 *
 *     //composite natural id
 *
 *     &#64;Enumerated
 *     &#64;NotNull &#64;NaturalId
 *     Region region;
 *
 *     &#64;NotNull &#64;NaturalId
 *     String registration;
 *
 *     ...
 * }
 * </pre>
 * <p>
 * Unlike the {@linkplain jakarta.persistence.Id primary identifier}
 * of an entity, a natural id may be {@linkplain #mutable}.
 * <p>
 * On the other hand, a field or property which forms part of a natural
 * id may never be null, and so it's a good idea to use {@code @NaturalId}
 * in conjunction with the Bean Validation {@code @NotNull} annotation
 * or {@link jakarta.persistence.Basic#optional @Basic(optional=false)}.
 * <p>
 * The {@link org.hibernate.Session} interface offers several methods
 * that allow an entity instance to be retrieved by its
 * {@linkplain org.hibernate.Session#bySimpleNaturalId(Class) simple}
 * or {@linkplain org.hibernate.Session#byNaturalId(Class) composite}
 * natural id value. If the entity is also marked for {@linkplain
 * NaturalIdCache natural id caching}, then these methods may be able
 * to avoid a database round trip.
 *
 * @author Nicolás Lichtmaier
 *
 * @see NaturalIdCache
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface NaturalId {
	/**
	 * Specifies whether the natural id is mutable or immutable.
	 *
	 * @return {@code false} (the default) indicates it is immutable;
	 *         {@code true} indicates it is mutable.
	 */
	boolean mutable() default false;
}
