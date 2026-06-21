/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.quarkus;

import jakarta.persistence.Entity;

@Entity
public class Panache2Child extends Panache2Parent {
	public String childName;
}
