/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.embeddable;

import jakarta.persistence.Entity;

@Entity
public class Stuff extends Base implements IStuff {
}
