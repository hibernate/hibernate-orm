/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
