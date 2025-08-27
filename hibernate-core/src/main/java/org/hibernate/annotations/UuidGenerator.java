/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.UUID;

import org.hibernate.Incubating;
import org.hibernate.id.uuid.UuidVersion6Strategy;
import org.hibernate.id.uuid.UuidVersion7Strategy;
import org.hibernate.id.uuid.UuidValueGenerator;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies that an entity identifier is generated as an
 * <a href=https://datatracker.ietf.org/doc/html/rfc4122>IETF RFC 4122 UUID</a>.
 * <p>
 * The type of the identifier attribute may be {@link UUID} or {@link String}.
 *
 * @see org.hibernate.id.uuid.UuidGenerator
 * @since 6.0
 *
 * @author Steve Ebersole
 */
@IdGeneratorType(org.hibernate.id.uuid.UuidGenerator.class)
@ValueGenerationType(generatedBy = org.hibernate.id.uuid.UuidGenerator.class)
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface UuidGenerator {

	/**
	 * Represents a kind of UUID, that is, what RFC 4122 calls a "version".
	 */
	enum Style {
		/**
		 * Defaults to {@link #RANDOM}.
		 */
		AUTO,
		/**
		 * Use {@link UUID#randomUUID()} to generate UUIDs, producing a value
		 * compatible with RFC 4122 version 4.
		 */
		RANDOM,
		/**
		 * Use a time-based generation strategy consistent with RFC 4122
		 * version 1, but with IP address instead of MAC address.
		 *
		 * @implNote Can be a bottleneck, since synchronization is used when
		 *           incrementing an internal counter as part of the algorithm.
		 */
		TIME,
		/**
		 * Use a time-based generation strategy consistent with RFC 4122
		 * version 6.
		 * @see UuidVersion6Strategy
		 */
		@Incubating
		VERSION_6,
		/**
		 * Use a time-based generation strategy consistent with RFC 4122
		 * version 7.
		 * @see UuidVersion7Strategy
		 */
		@Incubating
		VERSION_7
	}

	/**
	 * Specifies which {@linkplain Style style} of UUID generation should be used.
	 */
	Style style() default Style.AUTO;

	/**
	 * Allows to provide a specific, generally custom, value generation implementation.
	 *
	 * @apiNote If algorithm is specified, it is expected that {@linkplain #style()} be
	 * {@linkplain Style#AUTO}.
	 */
	@Incubating
	Class<? extends UuidValueGenerator> algorithm() default UuidValueGenerator.class;
}
