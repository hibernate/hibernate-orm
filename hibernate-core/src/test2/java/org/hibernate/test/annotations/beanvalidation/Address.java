/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.annotations.beanvalidation;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Entity
public class Address {
	@NotNull
	public static String blacklistedZipCode;

	private String line1;
	private String line2;
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

	@NotNull
	public String getLine1() {
		return line1;
	}

	public void setLine1(String line1) {
		this.line1 = line1;
	}

	public String getLine2() {
		return line2;
	}

	public void setLine2(String line2) {
		this.line2 = line2;
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
