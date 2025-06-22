/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.discovery.simple;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.type.NumericBooleanConverter;

/**
 * @author Steve Ebersole
 */
@Entity(name = "NumericEntity")
@Table(name = "numeric_entity")
@SoftDelete(converter = NumericBooleanConverter.class)
public class NumericEntity {
	@Id
	private Integer id;
	private String name;
}
