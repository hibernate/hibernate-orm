/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.xml.ejb3;

import jakarta.persistence.Column;
import java.util.Date;

/**
 * @author Emmanuel Bernard
 */
public class CarModel extends Model {
	@Column(name="model_year")
	private Date year;

	public Date getYear() {
		return year;
	}

	public void setYear(Date year) {
		this.year = year;
	}
}
