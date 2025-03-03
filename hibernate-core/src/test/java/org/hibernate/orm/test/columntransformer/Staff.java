/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.columntransformer;
import java.util.List;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.ColumnTransformer;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name="t_staff")
public class Staff {

	/**
	 * For Hibernate
	 */
	private Staff() {
	}

	public Staff(Integer id) {
		this( -1, -1, -1, id );
	}

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

	@Column(name="kooky")
	@ColumnTransformer(
			read = "cast( kooky as VARCHAR(255) )"
	)
	public String getKooky() { return kooky; }
	public void setKooky(String kooky) { this.kooky = kooky; }
	private String kooky;

	@ElementCollection
	@CollectionTable( name = "integers" )
	@Column( name = "integer_val" )
	@ColumnTransformer( forColumn = "integer_val", read = "integer_val + 20", write = "? - 20")
	public List<Integer> getIntegers() { return integers; }
	public void setIntegers(List<Integer> integers) { this.integers = integers; }
	private List<Integer> integers;

	@ElementCollection
	@CollectionTable( name = "integers2" )
	@Column( name = "integer_val2" )
	public List<Integer> getIntegers2() { return integers2; }
	public void setIntegers2(List<Integer> integers2) { this.integers2 = integers2; }
	private List<Integer> integers2;
}
