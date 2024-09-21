/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.join.namingstrategy;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SecondaryTable;
import java.io.Serializable;

/**
 * @author Sergey Vasilyev
 */
@Entity
@SecondaryTable(name = "ExtendedLife")
public class Life implements Serializable {
	@Id
	@GeneratedValue
	public Integer id;

	public int duration;
	@Column(table = "ExtendedLife")
	public String fullDescription;

	@ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	@JoinColumn(name = "CAT_ID", table = "ExtendedLife")
	public SimpleCat owner;

}
