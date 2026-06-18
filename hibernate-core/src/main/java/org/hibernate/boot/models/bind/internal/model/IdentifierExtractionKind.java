/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.model;

/// How one semantic identifier contribution moves values between the entity
/// virtual id shape and the public id representation.
///
/// @since 9.0
/// @author Steve Ebersole
public enum IdentifierExtractionKind {
	/// The public identifier value is taken directly from the semantic
	/// identifier attribute.
	///
	/// This is the normal case for basic identifier attributes, embedded
	/// identifier attributes copied into an `IdClass` representation, and any
	/// other identifier contribution where the entity-side virtual value and the
	/// public id representation describe the same value.
	///
	/// ```java
	/// @Entity
	/// class Customer {
	///     @Id Long id;
	/// }
	/// ```
	///
	/// ```java
	/// @Entity
	/// @IdClass(OrderLineId.class)
	/// class OrderLine {
	///     @Id Long orderId;
	///     @Id Integer lineNumber;
	/// }
	///
	/// class OrderLineId {
	///     Long orderId;
	///     Integer lineNumber;
	/// }
	/// ```
	DIRECT,

	/// The semantic identifier attribute is association-valued, but the public
	/// identifier representation stores the associated entity's identifier.
	///
	/// For example, an entity may expose `@Id @ManyToOne Customer customer`
	/// while its `IdClass` exposes `Long customer`.  Binding still needs the
	/// entity-side association for the virtual id shape, but extraction/injection
	/// must move the target entity identifier into and out of the public id
	/// object.
	///
	/// ```java
	/// @Entity
	/// @IdClass(MembershipId.class)
	/// class Membership {
	///     @Id @ManyToOne Customer customer;
	///     @Id String groupCode;
	/// }
	///
	/// class MembershipId {
	///     Long customer;
	///     String groupCode;
	/// }
	/// ```
	ASSOCIATION_TARGET_ID,

	/// The public identifier representation is the entire identifier of the
	/// associated target entity.
	///
	/// This covers whole derived-id mappings where the entity-side semantic
	/// identifier is an association and the public id representation corresponds
	/// to the target entity's complete id rather than to one scalar association
	/// id part.
	///
	/// ```java
	/// @Entity
	/// class UserProfile {
	///     @Id @OneToOne User user;
	/// }
	///
	/// // UserProfile's id representation is User's whole identifier.
	/// ```
	WHOLE_TARGET_ID
}
