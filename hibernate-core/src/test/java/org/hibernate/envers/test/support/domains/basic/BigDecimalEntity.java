/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.basic;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * @author Chris Cranford
 */
@Entity
@Audited
public class BigDecimalEntity {
	@Id
	@GeneratedValue
	private Integer id;
	@Column(precision = 23, scale = 6)
	private BigDecimal bigDecimal;
	@NotAudited
	private String data;

	public BigDecimalEntity() {

	}

	public BigDecimalEntity(Double bigDecimalValue, String data) {
		setBigDecimalValue( bigDecimalValue );
		this.data = data;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public BigDecimal getBigDecimal() {
		return bigDecimal;
	}

	public void setBigDecimal(BigDecimal bigDecimal) {
		this.bigDecimal = bigDecimal;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public void setBigDecimalValue(Double bigDecimalValue) {
		setBigDecimal( BigDecimal.valueOf( bigDecimalValue ) );
	}
}
