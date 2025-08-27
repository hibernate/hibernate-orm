/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.discovery.simple;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.SoftDelete;

/**
 * @author Steve Ebersole
 */
@Entity(name = "BooleanEntity")
@Table(name = "boolean_entity")
@SoftDelete()
public class BooleanEntity {
	@Id
	private Integer id;
	private String name;
}
