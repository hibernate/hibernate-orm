/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.union;

import jakarta.persistence.Entity;

/**
 * @author Steve Ebersole
 */
@Entity
public class UnionSub extends UnionRoot {
	private String unionData;
}
