/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.eg;

import jakarta.persistence.Embeddable;

@Embeddable
public record Address(String street, String city, String postcode) {}
