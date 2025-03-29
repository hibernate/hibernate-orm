/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;
import java.io.Serializable;
import java.util.Date;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;


/**
 * Corporate like Air France
 *
 * @author Emmanuel Bernard
 */
@Entity(name = "Corporation")
public class Company implements Serializable {
	private Integer id;
	private String name;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}


	public void setId(Integer integer) {
		id = integer;
	}


	public void setName(String string) {
		name = string;
	}

	//should be treated as getter
	private int[] getWorkingHoursPerWeek(Set<Date> holidayDays) {
		return null;
	}
}
