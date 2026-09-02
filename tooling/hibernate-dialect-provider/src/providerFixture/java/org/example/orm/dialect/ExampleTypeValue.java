/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

/// Immutable value used to demonstrate an external Java/JDBC type pairing.
///
/// @author Steve Ebersole
public record ExampleTypeValue(String text) {
}
