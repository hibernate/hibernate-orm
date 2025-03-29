/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.foreignkeys;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * @author Andrea Boriero
 */
@Entity
@Table(name = "GROUP")
public class Group implements Serializable {
	@Id
	@Column(name = "GROUP_ID")
	private Long id;
}
