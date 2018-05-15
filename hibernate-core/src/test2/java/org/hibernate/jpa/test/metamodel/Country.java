/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metamodel;
import javax.persistence.Basic;
import javax.persistence.Embeddable;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Embeddable
public class Country implements java.io.Serializable {
	private String country;
	private String code;

	public Country() {
	}

	public Country(String v1, String v2) {
		country = v1;
		code = v2;
	}

	@Basic
	public String getCountry() {
		return country;
	}

	public void setCountry(String v) {
		country = v;
	}

	@Basic
	public String getCode() {
		return code;
	}

	public void setCode(String v) {
		code = v;
	}
}

