/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/// Supported, read-only views of the semantic domain model produced during
/// mapping bootstrap.
///
/// The [mapping pipeline overview][org.hibernate.boot.mapping] describes the
/// categorization, binding, and materialization stages which produce these
/// views.
///
/// The contracts in this package expose the result of categorizing mapping
/// sources, before and while that semantic model is materialized as the mutable
/// boot model defined by `org.hibernate.mapping`. They are intended primarily
/// for bootstrap extensions, including [org.hibernate.binder.AttributeBinder]
/// and [org.hibernate.binder.TypeBinder].
///
/// ## Access from custom binders
///
/// Every binder context exposes the read-only categorized result through
/// `getDomainModel()`. [org.hibernate.boot.mapping.spi.CategorizedDomainModel]
/// is the navigation root for categorized metadata only; it is not a registry
/// of later declarations, usages, applications, or mutable boot mappings.
///
/// Hibernate owns the binding model, and there is no public binding-model root
/// or lookup service. An `org.hibernate.binder.AttributeBinder` obtains the
/// binding-side views from the `org.hibernate.binder.AttributeBindingContext`
/// supplied with its invocation:
///
/// ```java
/// AttributeApplication application = context.getAttribute();
/// AttributeUsage usage = application.usage();
/// AttributeDeclaration declaration = usage.declaration();
/// ```
///
/// The same context exposes the correlated mutable targets through
/// `getProperty()` and `getPersistentClass()`. Applications and usages are
/// invocation-scoped semantic views, not objects an extension looks up from the
/// categorized domain model.
///
/// Entity and embeddable `org.hibernate.binder.TypeBinder` invocations occur at
/// the corresponding materialized-type boundary. Their contexts expose
/// [org.hibernate.boot.mapping.spi.EntityTypeMetadata] or
/// [org.hibernate.boot.mapping.spi.EmbeddableUsageMetadata] together with the
/// mutable `org.hibernate.mapping.PersistentClass` or
/// `org.hibernate.mapping.Component`. They do not expose an arbitrary
/// [org.hibernate.boot.mapping.spi.AttributeApplication].
///
/// ## Attribute metadata, declaration, usage, and application
///
/// Four related contracts describe an attribute as it moves through bootstrap.
/// Consider a generic mapped superclass applied by an entity:
///
/// ```java
/// @MappedSuperclass
/// class Base<T> {
///     T value;
/// }
///
/// @Entity
/// class Customer extends Base<UUID> {
/// }
/// ```
///
/// Categorization first produces an [org.hibernate.boot.mapping.spi.EntityHierarchy] whose
/// [absolute root][org.hibernate.boot.mapping.spi.EntityHierarchy#getAbsoluteRoot()] is the mapped-superclass
/// metadata for `Base` and whose [entity root][org.hibernate.boot.mapping.spi.EntityHierarchy#getRoot()]
/// is the entity metadata for `Customer`. The attribute itself is represented by the following related objects.
///
/// ### `AttributeMetadata`: what was categorized
///
/// `Base` owns one [org.hibernate.boot.mapping.spi.AttributeMetadata] for
/// `value`. `Customer` does not own a second metadata object for the inherited
/// member. The metadata says:
///
/// - the persistent member is `Base.value`;
/// - its attribute name is `value`;
/// - its nature is `BASIC`; and
/// - its type at the declaration site is the type variable `T`.
///
/// It may resolve `T` against a later usage scope, but that does not change the
/// metadata or transfer its ownership from `Base` to `Customer`:
///
/// ```java
/// MappedSuperclassTypeMetadata base =
///         (MappedSuperclassTypeMetadata) hierarchy.getAbsoluteRoot();
/// EntityTypeMetadata customer = hierarchy.getRoot();
///
/// AttributeMetadata metadata = base.findAttribute("value");
/// TypeDetails declaredType = metadata.getAttributeType(); // T
/// TypeDetails resolvedType =
///         metadata.resolveAttributeType(customer.getClassDetails()); // UUID
/// ```
///
/// For a singular attribute, [org.hibernate.boot.mapping.spi.SingularAttributeMetadata]
/// additionally exposes the categorized [org.hibernate.boot.mapping.spi.ValueMetadata].
/// In this example that value is basic and its declaration-site Java type is
/// also `T`.
///
/// ### `AttributeDeclaration`: where the attribute originates
///
/// Binding produces one [org.hibernate.boot.mapping.spi.AttributeDeclaration]
/// from that categorized metadata. It is still about `Base.value`, not
/// `Customer.value`. Its significant state is conceptually:
///
/// ```java
/// declaration.attributeName()       == "value"
/// declaration.member()              == field("Base.value")
/// declaration.declarationRole()     == new DeclarationRole(
///         Base.class.getName(), "value")
/// declaration.nature()              == AttributeNature.BASIC
/// ```
///
/// The declaration also retains the access strategy used for the source
/// member. It contains neither `UUID` nor a `Customer` mapping role. If another
/// entity extends `Base<String>`, both entities still share this one
/// declaration and its `Base.value` declaration role.
///
/// ### `AttributeUsage`: how the declaration is interpreted
///
/// Binding then produces an [org.hibernate.boot.mapping.spi.AttributeUsage]
/// for the inheritance of `Base.value` by `Customer`. That usage points back
/// to the declaration above, but adds the `Customer`-specific interpretation:
///
/// ```java
/// usage.declaration()                == declaration
/// usage.attributeName()              == "value"
/// usage.member()                     == member("Base.value")
/// usage.resolvedType()               == type(UUID.class)
/// usage.sourceRole()                 == Customer.class.getName() + ".value"
/// usage.attributePath()              == "value"
/// usage.nature()                     == AttributeNature.BASIC
/// ```
///
/// Thus `T` belongs to the declaration-side metadata, while `UUID` belongs to
/// this usage. An entity extending `Base<String>` would produce a separate
/// usage referring to the same declaration, with `String` as its resolved
/// type and its own source role.
///
/// ### `AttributeApplication`: where the usage was materialized
///
/// Materializing the `Customer` usage produces one
/// [org.hibernate.boot.mapping.spi.AttributeApplication]. The application
/// correlates the usage with the concrete occurrence in the mutable boot
/// mapping graph:
///
/// ```java
/// application.usage()                == usage
/// application.declaration()          == declaration
/// application.declarationRole()      == declaration.declarationRole()
/// application.role()                 == MappingRole
///         .entity(customer.getEntityName())
///         .appendAttribute("value")
/// application.containerRole()        == MappingRole
///         .entity(customer.getEntityName())
/// application.resolvedType()         == type(UUID.class)
/// ```
///
/// The declaration role answers “which source member did this come from?”;
/// the mapping role answers “which concrete boot mapping occurrence is this?”
/// The mutable `org.hibernate.mapping.Property` for `Customer.value` carries
/// both identities, while the application remains a read-only semantic
/// description of that property.
///
/// An attribute binder receives this application together with its mutable
/// target:
///
/// ```java
/// public void bind(MyAnnotation annotation, AttributeBindingContext context) {
///     AttributeApplication application = context.getAttribute();
///     AttributeUsage usage = application.usage();
///     AttributeDeclaration declaration = application.declaration();
///
///     // Read metadata through the application...
///     Class<?> javaType =
///             usage.resolvedType().determineRawClass().toJavaClass();
///
///     // ...and customize the correlated mutable boot-model object.
///     Property bootProperty = context.getProperty();
/// }
/// ```
///
/// In short, metadata describes `Base.value` as `T`; the declaration gives
/// that source member a stable identity; the usage interprets `T` as `UUID`
/// for `Customer`; and the application assigns that usage a stable place in
/// `Customer`'s materialized mapping.
///
/// The broader _categorized_ model is navigable from [org.hibernate.boot.mapping.spi.CategorizedDomainModel]
/// through [org.hibernate.boot.mapping.spi.EntityHierarchy], [org.hibernate.boot.mapping.spi.ManagedTypeMetadata],
/// [org.hibernate.boot.mapping.spi.AttributeMetadata], and [org.hibernate.boot.mapping.spi.ValueMetadata].
/// [org.hibernate.boot.mapping.spi.DeclarationRole] and [org.hibernate.boot.mapping.spi.MappingRole] provide immutable,
/// serializable identities shared by these semantic descriptions and the mutable boot mapping model.
///
/// Instances of the metadata contracts are produced and owned by Hibernate.
/// Extensions must treat them as immutable. Collections and maps returned by
/// these contracts are read-only snapshots or views and must not be mutated.
/// Categorization services, mutable binding state, contribution records,
/// materializers, and internal resolution operations are deliberately not part
/// of this SPI.
///
/// This model exists during bootstrap. It is not the runtime mapping metamodel
/// and should not be retained as application state after bootstrap completes.
///
/// @since 9.0
/// @author Steve Ebersole
@Incubating
package org.hibernate.boot.mapping.spi;

import org.hibernate.Incubating;
