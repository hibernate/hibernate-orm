/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.inheritance.discriminatoroptions;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * @author Hardy Ferentschik
 */
@Entity
@DiscriminatorValue("B")
public class SubClass extends BaseClass {

}
