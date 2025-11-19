/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;

/// Models a non-aggregated composite natural-id for the purpose of loading.
/// The non-aggregated form uses multiple [@NaturalId][NaturalId] as opposed
/// to the aggregated form which uses a single [@NaturalId][NaturalId] combined
/// with [@Embedded][jakarta.persistence.Embedded].
/// Functions in a similar fashion as [@IdClass][jakarta.persistence.IdClass] for
/// non-aggregated composite identifiers.
///
/// ```java
/// @Entity
/// @NaturalIdClass(OrderNaturalId.class)
/// class Order {
///     @Id
/// 	Integer id;
/// 	@NaturalId @ManyToOne
/// 	Customer customer;
/// 	@NaturalId
/// 	Integer orderNumber;
///     ...
/// }
///
/// class OrderNaturalId {
/// 	Customer customer;
/// 	Integer orderNumber;
/// 	...
/// }
/// ```
///
/// @see NaturalId
/// @see jakarta.persistence.IdClass
///
/// @since 7.3
///
/// @author Steve Ebersole
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NaturalIdClass {
	/// The class to use for loading the associated entity by natural-id.
	Class<?> value();
}
