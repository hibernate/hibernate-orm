/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.records;

import jakarta.persistence.Embeddable;

@Embeddable
public record Address(String street, String city, String postalCode) {
}
