/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
 * synthetic identifier, with no domain-model semantics. Then should
 * always be some other field or combination of fields which uniquely
 * identifies an instance of the entity from the point of view of the
 * user of the system. This is the <em>natural id</em> of the entity.
 * <p>
 * The {@link org.hibernate.Session} interface offers several methods
 * that allow an entity instance to be retrieved by its
 * {@linkplain org.hibernate.Session#bySimpleNaturalId(Class) simple}
 * or {@linkplain org.hibernate.Session#byNaturalId(Class) composite}
 * natural id value. If the entity is also marked for {@linkplain
 * NaturalIdCache natural id caching}, then these methods may be able
 * to avoid a database round trip.
 *
 * @author Nicol�s Lichtmaier
 *
 * @see NaturalIdCache
 */
@Target( { METHOD, FIELD } )
@Retention( RUNTIME )
public @interface NaturalId {
	/**
	 * Specifies whether the natural id is mutable or immutable.
	 *
	 * @return {@code false} (the default) indicates it is immutable;
	 *         {@code true} indicates it is mutable.
	 */
	boolean mutable() default false;
}
