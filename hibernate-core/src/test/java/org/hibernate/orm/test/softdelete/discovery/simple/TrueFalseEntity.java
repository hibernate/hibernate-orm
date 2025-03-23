/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.discovery.simple;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.type.TrueFalseConverter;

/**
 * @author Steve Ebersole
 */
@Entity(name = "TrueFalseEntity")
@Table(name = "true_false_entity")
@SoftDelete(converter = TrueFalseConverter.class)
public class TrueFalseEntity {
	@Id
	private Integer id;
	private String name;
}
