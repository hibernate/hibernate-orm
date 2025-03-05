/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.fkcircularity;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * Test entities ANN-730.
 *
 * @author Hardy Ferentschik
 *
 */
@Entity
@Table(name = "class_b")
@PrimaryKeyJoinColumn(name = "id", referencedColumnName = "id")
public class ClassB extends ClassA {
}
