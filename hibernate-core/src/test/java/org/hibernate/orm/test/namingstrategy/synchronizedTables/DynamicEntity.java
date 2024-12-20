/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy.synchronizedTables;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Synchronize;

/**
 * @author Steve Ebersole
 */
@Entity
@Synchronize( value = "table_a" )
public class DynamicEntity {
	@Id
	public Integer id;
}
