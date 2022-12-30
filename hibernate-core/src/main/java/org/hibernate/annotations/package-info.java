/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * A set of mapping annotations which extend the O/R mapping annotations defined by JPA.
 *
 * <h3 id="basic-value-mapping">Basic value type mappings</h3>
 *
 * A <em>basic type</em> handles the persistence of an attribute of an entity or embeddable
 * object that is stored in exactly one database column.
 * <p>
 * JPA supports a very limited set of built-in {@linkplain jakarta.persistence.Basic basic}
 * types.
 * <table style="border-spacing: 10px">
 * <thead style="font-weight:bold">
 * <tr>
 * <td>Category</td><td>Package</td><td>Types</td>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 *     <td>Primitive types</td>
 *     <td></td>
 *     <td>{@code boolean}, {@code int}, {@code double}, etc.</td>
 * </tr>
 * <tr>
 *     <td>Primitive wrappers</td>
 *     <td>{@code java.lang}</td>
 *     <td>{@code Boolean}, {@code Integer}, {@code Double}, etc.</td>
 * </tr>
 * <tr>
 *     <td>Strings</td><td>{@code java.lang}</td>
 *     <td>{@code String}</td>
 * </tr>
 * <tr>
 *     <td>Arbitrary-precision numeric types</td>
 *     <td>{@code java.math}</td><td>{@code BigInteger}, {@code BigDecimal}</td>
 * </tr>
 * <tr>
 *     <td>Date/time types</td><td>{@code java.time}</td>
 *     <td>{@code LocalDate}, {@code LocalTime}, {@code LocalDateTime}, {@code OffsetDateTime}, {@code Instant}</td>
 * </tr>
 * <tr>
 *     <td>Deprecated date/time types</td>
 *     <td>{@code java.util}</td>
 *     <td>{@code Date}, {@code Calendar}</td>
 * </tr>
 * <tr>
 *     <td>Deprecated JDBC date/time types</td>
 *     <td>{@code java.sql}</td>
 *     <td>{@code Date}, {@code Time}, {@code Timestamp}</td>
 * </tr>
 * <tr>
 *     <td>Binary and character arrays</td>
 *     <td></td>
 *     <td>{@code byte[]}, {@code char[]}</td>
 * </tr>
 * <tr>
 *     <td>UUIDs</td><td>{@code java.util}</td>
 *     <td>{@code UUID}</td>
 * </tr>
 * <tr>
 *     <td>Enumerated types</td>
 *     <td></td>
 *     <td>Any {@code enum}</td>
 * </tr>
 * <tr>
 *     <td>Serializable types</td>
 *     <td></td>
 *     <td>Any {@code java.io.Serializable}</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * JPA provides only {@linkplain jakarta.persistence.AttributeConverter converters} as a
 * solution when the built-in types are insufficient.
 * <p>
 * By contrast, Hibernate has an embarrassingly rich set of abstractions for modelling
 * basic types, which can be initially confusing. Note that the venerable interface
 * {@link org.hibernate.type.Type} abstracts over all sorts of field and property types,
 * not only basic types. In modern Hibernate, programs should avoid directly implementing
 * this interface.
 * <p>
 * Instead, a program should use either a "compositional" basic type, or in more extreme
 * cases, a {@code UserType}.
 * <ul>
 * <li>
 *     A basic type is a composition of a {@link org.hibernate.type.descriptor.java.JavaType}
 *     with a {@link org.hibernate.type.descriptor.jdbc.JdbcType}, and possibly a JPA
 *     {@link jakarta.persistence.AttributeConverter}, and the process of composition is
 *     usually somewhat implicit.
 *     <ol>
 *     <li>A converter may be selected using the JPA {@link jakarta.persistence.Convert}
 *         annotation.
 *     <li>A {@code JavaType} or {@code JdbcType} may be indicated <em>explicitly</em>
 *         using the following annotations:
 *         <ul>
 *         <li>{@link org.hibernate.annotations.JavaType}
 *         <li>{@link org.hibernate.annotations.JdbcType}
 *         <li>{@link org.hibernate.annotations.JdbcTypeCode}
 *         </ul>
 *     <li>But these annotation also influence the choice:
 *         <ul>
 *         <li>{@link jakarta.persistence.Lob}
 *         <li>{@link jakarta.persistence.Enumerated}
 *         <li>{@link jakarta.persistence.Temporal}
 *         <li>{@link org.hibernate.annotations.Nationalized}
 *         </ul>
 *     <li>A compositional type mapping also comes with a
 *         {@link org.hibernate.type.descriptor.java.MutabilityPlan}, which is usually
 *         chosen by the {@code JavaType}, but which may be overridden using the
 *         {@link org.hibernate.annotations.Mutability} annotation.
 *     </ol>
 *     <p>
 *     Note that {@link org.hibernate.annotations.JavaType}, {@link org.hibernate.annotations.JdbcType},
 *     {@link org.hibernate.annotations.JdbcTypeCode} and {@link org.hibernate.annotations.Mutability}
 *     all come in specialized flavors for handling map keys, list indexes, and so on.
 * <li>
 *     Alternatively, a program may implement the {@link org.hibernate.usertype.UserType}
 *     interface and associate it with a field or property explicitly using the
 *     {@link org.hibernate.annotations.Type @Type} annotation, or implicitly using the
 *     {@link org.hibernate.annotations.TypeRegistration @TypeRegistration} annotation.
 *     There are some specialized flavors of the {@code @Type} annotation too.
 *     </li>
 * </ul>
 * <p>
 * These two approaches cannot be used together. A {@code UserType} always takes precedence
 * over the compositional approach.
 * <p>
 * The packages {@link org.hibernate.type.descriptor.java} and
 * {@link org.hibernate.type.descriptor.jdbc} contain the built-in implementations of
 * {@code JavaType} and {@code JdbcType}, respectively.
 * <p>
 * Please see the <em>User Guide</em> or the package {@link org.hibernate.type} for further
 * discussion.
 *
 * <h3 id="second-level-cache">Second level cache</h3>
 *
 * When we make a decision to store an entity in the second-level cache, we must decide
 * much more than just whether "to cache or not to cache". Among other considerations:
 * <ul>
 * <li>we must assign cache management policies like an expiry timeout, whether to use
 *     FIFO-based eviction, whether cached items may be serialized to disk, and
 * <li>we must also take great care in specifying how
 *     {@linkplain org.hibernate.annotations.CacheConcurrencyStrategy concurrent access}
 *     to cached items is managed.
 * </ul>
 * <p>
 * In a multi-user system, these policies always depend quite sensitively on the nature
 * of the given entity type, and cannot reasonably be fixed at a more global level.
 * <p>
 * With all the above considerations in mind, we strongly recommend the use of the
 * Hibernate-defined annotation {@link org.hibernate.annotations.Cache} to assign
 * entities to the second-level cache.
 * <p>
 * The JPA-defined {@link jakarta.persistence.Cacheable} annotation is almost useless
 * to us, since:
 * <ul>
 * <li>it provides no way to specify any information about the nature of the <em>how</em>
 *     cached entity and how its cache should be managed, and
 * <li>it may not be used to annotate associations.
 * </ul>
 * <p>
 * As an aside, the {@link jakarta.persistence.SharedCacheMode} enumeration is even worse:
 * its only sensible values are {@code NONE} and {@code ENABLE_SELECTIVE}. The options
 * {@code ALL} and {@code DISABLE_SELECTIVE} fit extremely poorly with the practices
 * advocated above.
 *
 * <h3 id="filters">Filters</h3>
 *
 * Filters are an extremely powerful feature of Hibernate, allowing the definition of
 * parameterized families of filtered "views" of the domain data. They're also easy
 * to use, with the minor caveat that they require the developer to express filtering
 * expressions in native SQL.
 * <ul>
 * <li>The {@link org.hibernate.annotations.FilterDef} annotation defines a named
 *     filter, declares its parameters, and might specify a filtering expression
 *     used by default. There should be exactly one of these annotations per filter
 *     name.
 * <li>The {@link org.hibernate.annotations.Filter} annotation is used to identify
 *     which entities and associations are affected by the filter, and provide a
 *     more specific filtering condition.
 * </ul>
 * <p>
 * Note that a filter has no affect unless it is
 * {@linkplain org.hibernate.Session#enableFilter(java.lang.String) enabled} in a
 * particular session.
 *
 * <h3 id="dialect-specific-sql">Dialect-specific native SQL</h3>
 *
 * Many annotations in this package allow the specification of native SQL expressions or
 * even complete statements. For example:
 * <ul>
 * <li>{@link org.hibernate.annotations.Formula} allows a field or property to map to an
 *     arbitrary SQL expression instead of a column,
 * <li>{@link org.hibernate.annotations.Check} specifies a check constraint condition,
 * <li>{@link org.hibernate.annotations.ColumnDefault} specifies default value, and
 *     {@link org.hibernate.annotations.GeneratedColumn} specifies a generated value,
 * <li>{@link org.hibernate.annotations.Filter} and {@link org.hibernate.annotations.Where}
 *     each specify a restriction written in SQL,
 * <li>{@link org.hibernate.annotations.OrderBy} specifies an ordering written in SQL, and
 * <li>{@link org.hibernate.annotations.SQLUpdate}, {@link org.hibernate.annotations.SQLInsert},
 *     and {@link org.hibernate.annotations.SQLDelete} allow a whole handwritten SQL statement
 *     to be given in place of the SQL generated by Hibernate.
 * </ul>
 * <p>
 * A major disadvantage to annotation-based mappings for programs which target multiple databases
 * is that there can be only one source of metadata which must work on every supported database.
 * Fortunately, there's a&mdash;slightly inelegant&mdash;solution.
 * <p>
 * The annotations belonging to {@link org.hibernate.annotations.DialectOverride} allow native
 * SQL to be overridden for a particular {@linkplain org.hibernate.dialect.Dialect SQL dialect}.
 * For example {@link org.hibernate.annotations.DialectOverride.Formula @DialectOverride.Formula}
 * may be used to customize a {@link org.hibernate.annotations.Formula @Formula} for a given version
 * of a given database.
 */
package org.hibernate.annotations;
