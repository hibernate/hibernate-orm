/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.foreignkeys.sorting;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

/**
 * @author Steve Ebersole
 */
@Entity
public class B {
	enum Type {ONE,ANOTHER}

	@Id
	@Column(length = 12)
	String code;

	@Id
	@Column(precision = 10, scale = 0)
	BigDecimal cost;

	@Id
	@Enumerated(EnumType.STRING)
	Type type;

	@Id
	long id;

	String name;

	public B() {
	}

	public B(String code, BigDecimal cost, Type type, long id, String name) {
		this.code = code;
		this.cost = cost;
		this.type = type;
		this.id = id;
		this.name = name;
	}
}
