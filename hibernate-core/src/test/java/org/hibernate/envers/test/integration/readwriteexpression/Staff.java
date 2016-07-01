/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.readwriteexpression;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "t_staff")
public class Staff {

	public Staff() {
	}

	public Staff(double sizeInInches, Integer id) {
		this.sizeInInches = sizeInInches;
		this.id = id;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	private Integer id;

	@Audited
	@Column(name = "size_in_cm")
	@ColumnTransformer(
			forColumn = "size_in_cm",
			read = "size_in_cm / 2.54E0",
			write = "? * 2.54E0")
	public double getSizeInInches() {
		return sizeInInches;
	}

	public void setSizeInInches(double sizeInInches) {
		this.sizeInInches = sizeInInches;
	}

	private double sizeInInches;


}