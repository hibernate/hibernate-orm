/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.discovery.simple;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.type.YesNoConverter;

/**
 * @author Steve Ebersole
 */
@Entity(name = "YesNoEntity")
@Table(name = "yes_no_entity")
@SoftDelete(converter = YesNoConverter.class)
public class YesNoEntity {
	@Id
	private Integer id;
	private String name;
}
