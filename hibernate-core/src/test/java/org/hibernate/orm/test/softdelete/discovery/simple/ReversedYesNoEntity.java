/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.discovery.simple;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.type.YesNoConverter;

/**
 * @author Steve Ebersole
 */
@Entity(name = "ReversedYesNoEntity")
@Table(name = "reversed_yes_no_entity")
@SoftDelete(converter = YesNoConverter.class, strategy = SoftDeleteType.ACTIVE)
public class ReversedYesNoEntity {
	@Id
	private Integer id;
	private String name;
}
