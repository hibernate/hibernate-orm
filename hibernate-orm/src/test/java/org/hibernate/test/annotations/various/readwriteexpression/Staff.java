/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.various.readwriteexpression;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.ColumnTransformer;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name="t_staff")
public class Staff {

	public Staff(double sizeInInches, double radius, double diameter, Integer id) {
		this.sizeInInches = sizeInInches;
		this.radiusS = radius;
		this.diameter = diameter;
		this.id = id;
	}

	@Id
	public Integer getId() { return id; }
	public void setId(Integer id) {  this.id = id; }
	private Integer id;

	@Column(name="size_in_cm")
	@ColumnTransformer(
			forColumn = "size_in_cm",
			read = "size_in_cm / 2.54E0",
			write = "? * 2.54E0" )
	public double getSizeInInches() { return sizeInInches; }
	public void setSizeInInches(double sizeInInches) {  this.sizeInInches = sizeInInches; }
	private double sizeInInches;

	//Weird extra S to avoid potential SQL keywords
	@ColumnTransformer(
			read = "radiusS / 2.54E0",
			write = "? * 2.54E0" )
	public double getRadiusS() { return radiusS; }
	public void setRadiusS(double radiusS) {  this.radiusS = radiusS; }
	private double radiusS;

	@Column(name="diamet")
	@ColumnTransformer(
			read = "diamet / 2.54E0",
			write = "? * 2.54E0" )
	public double getDiameter() { return diameter; }
	public void setDiameter(double diameter) {  this.diameter = diameter; }
	private double diameter;
}
