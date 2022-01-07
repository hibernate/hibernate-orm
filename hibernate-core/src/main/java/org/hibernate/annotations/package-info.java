/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * Package containing all Hibernate's specific annotations.
 *
 * <h3 id="basic-value-mapping">Basic value mapping</h3>
 *
 * Hibernate supports 2 approaches to defining a mapping strategy for basic values:<ul>
 *     <li>
 *         A "compositional" approach using a combination of the following influencers for
 *         various parts of mapping<ul>
 *             <li>{@link org.hibernate.annotations.JavaType}</li>
 *             <li>{@link org.hibernate.annotations.JdbcType}</li>
 *             <li>{@link org.hibernate.annotations.JdbcTypeCode}</li>
 *             <li>{@link org.hibernate.annotations.Mutability}</li>
 *             <li>{@link jakarta.persistence.AttributeConverter} / {@link jakarta.persistence.Convert}</li>
 *             <li>{@link jakarta.persistence.Lob}</li>
 *             <li>{@link jakarta.persistence.Enumerated}</li>
 *             <li>{@link jakarta.persistence.Temporal}</li>
 *             <li>{@link org.hibernate.annotations.Nationalized}</li>
 *         </ul>
 *         Note that {@link org.hibernate.annotations.JavaType}, {@link org.hibernate.annotations.JdbcType},
 *         {@link org.hibernate.annotations.JdbcTypeCode} and {@link org.hibernate.annotations.Mutability}
 *         all have specialized forms for the various model parts such as map-key, list-index, (id-bag)
 *         collection-id, etc.
 *     </li>
 *     <li>
 *          Contracted via the {@link org.hibernate.usertype.UserType} interface and specified using
 *          {@link org.hibernate.annotations.Type}.
 *          As with the compositional approach, there are model-part specific annotations for specifying
 *          custom-types as well.
 *     </li>
 * </ul>
 *
 * These 2 approaches are not intended to be mixed.  Specifying a custom-type takes precedence over
 * the compositional approach.  Though the compositional approach is recommended, both forms are
 * fully supported.
 *
 * See the user-guide for a more in-depth discussion
 */
package org.hibernate.annotations;
