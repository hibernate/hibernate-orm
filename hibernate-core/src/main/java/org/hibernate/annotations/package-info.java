/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * A set of mapping annotations which extend the O/R mapping annotations defined by JPA.
 * <p>
 * The JPA specification perfectly nails many aspects of the O/R persistence problem, but
 * here we address some areas where it falls short.
 *
 * <h3 id="basic-value-mapping">Basic types in JPA</h3>
 * <p>
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
 * JPA does provide {@linkplain jakarta.persistence.AttributeConverter converters} as an
 * extensibility mechanism, but its converters are only useful for classes which have an
 * equivalent representation as one of the types listed above.
 *
 * <h3 id="basic-value-mapping">Basic value type mappings</h3>
 * <p>
 * By contrast, Hibernate has an embarrassingly rich set of abstractions for modelling
 * basic types, which can be initially confusing.
 * <p>
 * Note that the venerable interface {@link org.hibernate.type.Type} abstracts over all
 * sorts of field and property types, not only basic types. In modern Hibernate, programs
 * should avoid direct use of this interface.
 * <p>
 * Instead, a program should use either a "compositional" basic type, or in more extreme
 * cases, a {@code UserType}.
 * <ul>
 * <li><p>
 *     A basic type is a composition of a {@link org.hibernate.type.descriptor.java.JavaType}
 *     with a {@link org.hibernate.type.descriptor.jdbc.JdbcType}, and possibly a JPA
 *     {@link jakarta.persistence.AttributeConverter}, and the process of composition is
 *     usually somewhat implicit.
 *     <ol>
 *     <li><p>
 *         A converter may be selected using the JPA {@link jakarta.persistence.Convert}
 *         annotation, or it may be {@linkplain jakarta.persistence.Converter#autoApply()
 *         applied implicitly}.
 *     <li><p>
 *         A {@code JavaType} or {@code JdbcType} may be indicated <em>explicitly</em>
 *         using the following annotations:
 *         <ul>
 *         <li>{@link org.hibernate.annotations.JavaType}
 *         <li>{@link org.hibernate.annotations.JdbcType}
 *         <li>{@link org.hibernate.annotations.JdbcTypeCode}
 *         </ul>
 *     <li><p>
 *         But these annotation also influence the choice:
 *         <ul>
 *         <li>{@link jakarta.persistence.Lob}
 *         <li>{@link jakarta.persistence.Enumerated}
 *         <li>{@link jakarta.persistence.Temporal}
 *         <li>{@link org.hibernate.annotations.Nationalized}
 *         </ul>
 *     <li><p>
 *         Furthermore, a {@link org.hibernate.annotations.JavaTypeRegistration} or
 *         {@link org.hibernate.annotations.JdbcTypeRegistration} allows the choice
 *         of {@code JavaType} or {@code JdbcType} to be made <em>implicitly</em>.
 *     <li><p>
 *         A compositional type mapping also comes with a
 *         {@link org.hibernate.type.descriptor.java.MutabilityPlan}, which is usually
 *         chosen by the {@code JavaType}, but which may be overridden using the
 *         {@link org.hibernate.annotations.Mutability} annotation.
 *     </ol>
 *     <p>
 *     Note that {@link org.hibernate.annotations.JavaType}, {@link org.hibernate.annotations.JdbcType},
 *     {@link org.hibernate.annotations.JdbcTypeCode} and {@link org.hibernate.annotations.Mutability}
 *     all come in specialized flavors for handling map keys, list indexes, and so on.
 * <li><p>
 *     Alternatively, a program may implement the {@link org.hibernate.usertype.UserType}
 *     interface and associate it with a field or property
 *     <ul>
 *     <li>explicitly, using the {@link org.hibernate.annotations.Type @Type} annotation,
 *         or
 *     <li>implicitly, using the {@link org.hibernate.annotations.TypeRegistration @TypeRegistration}
 *         annotation.
 *     </ul>
 *     <p>
 *     There are some specialized flavors of the {@code @Type} annotation too.
 * </ul>
 * <p>
 * These two approaches cannot be used together. A {@code UserType} always takes precedence
 * over the compositional approach.
 * <p>
 * All the typing annotations just mentioned may be used as meta-annotations. That is,
 * it's possible to define a new typing annotation like this:
 * <pre>
 * &#64;JavaType(ThingJavaType.class)
 * &#64;JdbcTypeCode(JSON)
 * &#64;Target({METHOD, FIELD})
 * &#64;Retention(RUNTIME)
 * public &#64;interface JsonThing {}
 * </pre>
 * The annotation may then be applied to fields and properties of entities and embeddable
 * objects:
 * <pre>
 * &#64;JsonThing Thing myThing;
 * </pre>
 * The packages {@link org.hibernate.type.descriptor.java} and
 * {@link org.hibernate.type.descriptor.jdbc} contain the built-in implementations of
 * {@code JavaType} and {@code JdbcType}, respectively.
 * <p>
 * See the <em>User Guide</em> or the package {@link org.hibernate.type} for further
 * discussion.
 *
 * <h3 id="composite-types">Composite types</h3>
 *
 * A <em>composite type</em> is a type which maps to multiple columns. An example of a
 * composite type is an {@linkplain jakarta.persistence.Embeddable embeddable} object,
 * but this is not the only sort of composite type in Hibernate.
 * <p>
 * A program may implement the {@link org.hibernate.usertype.CompositeUserType}
 * interface and associate it with a field or property:
 * <ul>
 * <li>explicitly, using the {@link org.hibernate.annotations.CompositeType @CompositeType}
 *     annotation, or
 * <li>implicitly, using the {@link org.hibernate.annotations.CompositeTypeRegistration @CompositeTypeRegistration}
 *     annotation.
 * </ul>
 *
 * <h3 id="second-level-cache">Second level cache</h3>
 * <p>
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
 * <li>it provides no way to specify any information about the nature of the cached
 *     entity and <em>how</em> its cache should be managed, and
 * <li>it may not be used to annotate associations.
 * </ul>
 * <p>
 * As an aside, the {@link jakarta.persistence.SharedCacheMode} enumeration is even worse:
 * its only sensible values are {@code NONE} and {@code ENABLE_SELECTIVE}. The options
 * {@code ALL} and {@code DISABLE_SELECTIVE} fit extremely poorly with the practices
 * advocated above.
 *
 * <h3 id="generated-values-natural-ids">Generated values</h3>
 * <p>
 * JPA supports {@linkplain jakarta.persistence.GeneratedValue generated} identifiers,
 * that is, surrogate primary keys, with four useful built-in
 * {@linkplain jakarta.persistence.GenerationType types} of id generation.
 * <p>
 * In JPA, an id generator is identified on the basis of a stringly-typed name, and
 * this provides a reasonably natural way to integrate
 * {@linkplain org.hibernate.annotations.GenericGenerator custom generators}.
 * <p>
 * JPA does not define any way to generate the values of other fields or properties
 * of the entity.
 * <p>
 * Hibernate 6 takes a different route, which is both more typesafe, and much more
 * extensible.
 * <ol>
 * <li>The interfaces {@link org.hibernate.generator.BeforeExecutionGenerator}
 *     and {@link org.hibernate.generator.OnExecutionGenerator} provide an extremely
 *     open-ended way to incorporate custom generators.
 * <li>The meta-annotations {@link org.hibernate.annotations.IdGeneratorType} and
 *     {@link org.hibernate.annotations.ValueGenerationType} may be used to associate
 *     a generator with a user-defined annotation. This annotation is an indirection
 *     between the generator itself, and the persistent attributes it generates.
 * <li>This <em>generator annotation</em> may then by used to annotate {@code @Id}
 *     attributes, {@code @Version} attributes, and other {@code @Basic} attributes to
 *     specify how their values are generated.
 * </ol>
 * <p>
 * This package includes a number built-in generator annotations, including
 * {@link org.hibernate.annotations.UuidGenerator},
 * {@link org.hibernate.annotations.CurrentTimestamp},
 * {@link org.hibernate.annotations.TenantId},
 * {@link org.hibernate.annotations.Generated}, and
 * {@link org.hibernate.annotations.GeneratedColumn}.
 *
 * <h3 id="natural-ids">Natural ids</h3>
 * <p>
 * The use of surrogate keys is highly recommended, making it much easier to evolve
 * a database schema over time. But every entity should also have a "natural" unique
 * key: a subset of fields which, taken together, uniquely identify an instance of
 * the entity in the business or scientific domain.
 * <p>
 * The {@link org.hibernate.annotations.NaturalId} annotation is used to identify
 * the natural key of an entity, and urge its use.
 * <p>
 * The {@link org.hibernate.annotations.NaturalIdCache} annotation enables the use
 * of the second-level cache for when an entity is loaded by natural id. Retrieval
 * by natural id is a very common thing to do, and so the cache can often be helpful.
 *
 * <h3 id="filters">Filters</h3>
 * <p>
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
 * <h3 id="optimistic-locking">Optimistic locking</h3>
 *
 * JPA defines the {@link jakarta.persistence.Version} annotation for optimistic
 * locking based on an integral version number or {@link java.sql.Timestamp}.
 * Hibernate allows this annotation to be used with other datetime types including
 * {@link java.time.Instant}.
 * <p>
 * A field may be explicitly excluded from optimistic lock checking using
 * {@link org.hibernate.annotations.OptimisticLock @OptimisticLock(excluded=true)}.
 * <p>
 * This standard JPA approach is the recommended approach when working with a
 * newly-designed database schema. But when working with a legacy database with
 * tables having no version or update timestamp column, an alternative approach is
 * supported:
 * <ul>
 * <li>{@link org.hibernate.annotations.OptimisticLockType#ALL @OptimisticLocking(ALL)}
 *     specifies that optimistic lock checking should be done by comparing the values
 *     of all columns, and
 * <li>{@link org.hibernate.annotations.OptimisticLockType#DIRTY @OptimisticLocking(DIRTY)}
 *     specifies that optimistic lock checking should be done by checking the values
 *     of only the columns which are being set to new values.
 * </ul>
 * <p>
 * For more detail, see {@link org.hibernate.annotations.OptimisticLocking}.
 *
 * <h3 id="dialect-specific-sql">Dialect-specific native SQL</h3>
 * <p>
 * Many annotations in this package allow the specification of native SQL expressions or
 * even complete statements. For example:
 * <ul>
 * <li>{@link org.hibernate.annotations.Formula} allows a field or property to map to an
 *     arbitrary SQL expression instead of a column,
 * <li>{@link org.hibernate.annotations.Check} specifies a check constraint condition,
 * <li>{@link org.hibernate.annotations.ColumnDefault} specifies a default value, and
 *     {@link org.hibernate.annotations.GeneratedColumn} specifies a generated value,
 * <li>{@link org.hibernate.annotations.Filter} and {@link org.hibernate.annotations.SQLRestriction}
 *     each specify a restriction written in SQL,
 * <li>{@link org.hibernate.annotations.SQLOrder} specifies an ordering written in SQL, and
 * <li>{@link org.hibernate.annotations.SQLSelect}, {@link org.hibernate.annotations.SQLUpdate},
 *     {@link org.hibernate.annotations.SQLInsert}, and {@link org.hibernate.annotations.SQLDelete}
 *     allow a whole handwritten SQL statement to be given in place of the SQL generated by Hibernate.
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
