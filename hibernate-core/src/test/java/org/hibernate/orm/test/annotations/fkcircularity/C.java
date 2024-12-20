/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.fkcircularity;
import jakarta.persistence.Entity;

/**
 * Test entities ANN-722.
 *
 * @author Hardy Ferentschik
 *
 */
@Entity
public class C extends B {
}
