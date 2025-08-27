/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id;

/**
 * An {@link IdentifierGenerator} that returns the current identifier assigned to an instance.
 *
 * @author Gavin King
 *
 * @implNote This also implements the {@code assigned} generation type in {@code hbm.xml} mappings.
 *
 * @deprecated replaced by {@link org.hibernate.generator.Assigned}
 */
@Deprecated(since = "7.0", forRemoval = true)
public class Assigned extends org.hibernate.generator.Assigned {
}
