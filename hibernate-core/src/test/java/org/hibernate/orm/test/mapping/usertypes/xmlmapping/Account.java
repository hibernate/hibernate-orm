/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.usertypes.xmlmapping;

import org.hibernate.annotations.Type;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Account {
	private Long id;
	private String name;
	private CurrencyUnit currency;

	public Account() {
	}

	public Account(String name, CurrencyUnit currency) {
		this.name = name;
		this.currency = currency;
	}

	@Id
	@GeneratedValue( strategy = GenerationType.AUTO )
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Type( value = CurrencyUnitUserType.class )
	@Column( length = 3 )
	public CurrencyUnit getCurrency() {
		return currency;
	}

	public void setCurrency(CurrencyUnit currency) {
		this.currency = currency;
	}
}
