/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.records;

import jakarta.persistence.Embeddable;

@Embeddable
public record Address(String street, String city, String postalCode) {
}
