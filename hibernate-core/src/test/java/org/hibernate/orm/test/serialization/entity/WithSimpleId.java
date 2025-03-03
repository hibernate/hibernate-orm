/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.serialization.entity;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * The class should be in a package that is different from the test
 * so that the test does not have access to the private ID field.
 *
 * @author Gail Badner
 */
@Entity
@Cacheable
public class WithSimpleId {
@Id
private Long id;
}
