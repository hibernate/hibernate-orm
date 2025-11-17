/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id.entities;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.TableGenerator;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class BreakDance {
	@Id
	@GeneratedValue(generator = "memencoIdGen", strategy = GenerationType.TABLE)
	@TableGenerator(
		name = "memencoIdGen",
		table = "hi_id_key",
		pkColumnName = "id_key",
		valueColumnName = "next_hi",
		pkColumnValue = "issue",
		allocationSize = 1
	)
	public Integer id;
	public String name;
}
