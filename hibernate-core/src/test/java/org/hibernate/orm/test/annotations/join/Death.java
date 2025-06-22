/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.join;
import java.io.Serializable;
import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;

/**
 * @author Emmanuel Bernard
 */
@Entity
@SecondaryTable(
		name = "ExtendedDeath",
		pkJoinColumns = @PrimaryKeyJoinColumn(name = "DEATH_ID")
)
public class Death implements Serializable {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Integer id;
	@Column(name = "death_date")
	public Date date;
	@Column(table = "ExtendedDeath")
	public String howDoesItHappen;
}
