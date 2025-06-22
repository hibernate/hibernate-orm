/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.beanvalidation;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.*;

@Entity
public class Address {
	@NotNull
	public static String blacklistedZipCode;

	private String line1;
	private String line2;
	private String line3;
	private String line4;
	private String line5;
	private String line6;
	private String line7;
	private String line8;
	private String line9;
	private String zip;
	private String state;
	@Size(max = 20)
	@NotNull
	private String country;
	private long id;
	private boolean internalValid = true;
	@Min(-2)
	@Max(value = 50)
	public int floor;

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	@NotEmpty
	public String getLine1() {
		return line1;
	}

	public void setLine1(String line1) {
		this.line1 = line1;
	}

	@NotBlank
	public String getLine2() {
		return line2;
	}

	public void setLine2(String line2) {
		this.line2 = line2;
	}

	@CustomNullOrNotBlank
	public String getLine3() {
		return line3;
	}

	public void setLine3(String line3) {
		this.line3 = line3;
	}

	@CustomNotNullOrNotBlank
	public String getLine4() {
		return line4;
	}

	public void setLine4(String line4) {
		this.line4 = line4;
	}

	@CustomNullAndNotBlank
	public String getLine5() {
		return line5;
	}

	public void setLine5(String line5) {
		this.line5 = line5;
	}

	@CustomNotNullAndNotBlank
	public String getLine6() {
		return line6;
	}

	public void setLine6(String line6) {
		this.line6 = line6;
	}

	@CustomNullOrPattern
	public String getLine7() {
		return line7;
	}

	public void setLine7(String line7) {
		this.line7 = line7;
	}

	@CustomNotNull
	public String getLine8() {
		return line8;
	}

	public void setLine8(String line8) {
		this.line8 = line8;
	}

	@CustomNull
	public String getLine9() {
		return line9;
	}

	public void setLine9(String line9) {
		this.line9 = line9;
	}

	@Size(max = 3)
	@NotNull
	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	@Size(max = 5)
	@Pattern(regexp = "[0-9]+")
	@NotNull
	public String getZip() {
		return zip;
	}

	public void setZip(String zip) {
		this.zip = zip;
	}

	@AssertTrue
	@Transient
	public boolean isValid() {
		return true;
	}

	@AssertTrue
	@Transient
	private boolean isInternalValid() {
		return internalValid;
	}

	public void setInternalValid(boolean internalValid) {
		this.internalValid = internalValid;
	}

	@Id
	@Min(1)
	@Max(2000)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
}
