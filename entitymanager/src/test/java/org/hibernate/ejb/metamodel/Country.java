/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ejb.metamodel;

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

