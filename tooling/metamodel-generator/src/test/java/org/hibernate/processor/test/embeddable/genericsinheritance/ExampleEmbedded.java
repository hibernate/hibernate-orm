/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.embeddable.genericsinheritance;

import jakarta.persistence.Embeddable;

@Embeddable
public class ExampleEmbedded<T> extends ExampleSuperClassEmbedded<T> {
}
