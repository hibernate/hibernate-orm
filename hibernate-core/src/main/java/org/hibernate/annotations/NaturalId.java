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

/// Specifies that a field or property of an entity class is part of
/// the natural id of the entity. This annotation is very useful when
/// the primary key of an entity class is a surrogate key, that is,
/// a {@linkplain jakarta.persistence.GeneratedValue system-generated}
/// synthetic identifier, with no domain-model semantics. There should
/// always be some other field or combination of fields that uniquely
/// identifies an instance of the entity from the point of view of the
/// user of the system. This is the _natural id_ of the entity.
///
/// A natural id may be a single (basic or embedded) attribute of the entity:
/// ````java
/// @Entity
/// class Person {
///
///     //synthetic id
///     @GeneratedValue @Id
///     Long id;
///
///     @NotNull
///     String name;
///
///     //simple natural id
///     @NotNull @NaturalId
///     String ssn;
///
///     ...
/// }
/// ```
///
/// or it may be a non-aggregated composite value:
/// ```java
/// @Entity
/// class Vehicle {
///
///     //synthetic id
///     @GeneratedValue @Id
///     Long id;
///
///     //composite natural id
///
///     @Enumerated
///     @NotNull
///	    @NaturalId
///     Region region;
///
///     @NotNull
///     @NaturalId
///     String registration;
///
///     ...
/// }
/// ```
///
/// Unlike the {@linkplain jakarta.persistence.Id primary identifier}
/// of an entity, a natural id may be {@linkplain #mutable}.
///
/// On the other hand, a field or property which forms part of a natural
/// id may never be null, and so it's a good idea to use `@NaturalId`
/// in conjunction with the Bean Validation `@NotNull` annotation
/// or [@Basic(optional=false)][jakarta.persistence.Basic#optional()].
///
/// The [org.hibernate.Session] interface offers several methods
/// that allow retrieval of one or more entity references by natural-id
/// allow an entity instance to be retrieved by its natural-id:
/// * [org.hibernate.Session#find(Class, Object, jakarta.persistence.FindOption...)]
///     allows loading a single entity instance by natural-id.
/// * [org.hibernate.Session#findMultiple(Class, java.util.List, jakarta.persistence.FindOption...)]
///     allows loading multiple entity instances by natural-id.
///
/// ```
/// Person person = session.find(Person.class, ssn, KeyType.NATURAL);
/// ```
/// ```
/// Vehicle vehicle =
///         session.find(Vehicle.class,
///                 Map.of(Vehicle_.REGION, region,
///                        Vehicle_.REGISTRATION, registration),
///                 KeyType.NATURAL);
/// ```
/// ```
/// List<Person> people = session.findMultiple(Person.class, ssns, KeyType.NATURAL);
/// ```
///
/// If the entity is also marked for [natural id caching][NaturalIdCache],
/// then these methods may be able to avoid a database round trip.
///
/// @see org.hibernate.Session#find(Class, Object, jakarta.persistence.FindOption...)
/// @see org.hibernate.Session#findMultiple(Class, java.util.List, jakarta.persistence.FindOption...)
/// @see NaturalIdClass
/// @see NaturalIdCache
///
/// @apiNote For non-aggregated composite natural-id cases, it is recommended to
/// leverage [@NaturalIdClass][NaturalIdClass] for loading.
/// ```
/// Vehicle vehicle =
///         session.find(Vehicle.class,
///                 VehicleKey(region, registration),
///                 KeyType.NATURAL);
/// ```
///
/// @author Nicolás Lichtmaier
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
