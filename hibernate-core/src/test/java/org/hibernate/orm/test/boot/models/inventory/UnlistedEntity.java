/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.inventory;

/// @author Steve Ebersole
@jakarta.persistence.Entity
public class UnlistedEntity {
	@jakarta.persistence.Id
	private Integer id;
}
