/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.jpa.convert;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Etienne Miret
 */
@Entity
public class Animal {

	@Id
	@GeneratedValue
	private Long id;

	@Convert( converter = BooleanCharConverter.class )
	private Boolean isFemale;

	@Convert( converter = NullToEmptyStringConverter.class )
	private String description;

	public Long getId() {
		return id;
	}

	/**
	 * Set to false on storing if {@code null}.
	 */
	public Boolean getIsFemale() {
		return isFemale;
	}

	public void setIsFemale(final Boolean isFemale) {
		this.isFemale = isFemale;
	}

	/**
	 * Set to the empty string if {@code null} on retrieving.
	 */
	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

}
