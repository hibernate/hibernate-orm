/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.support.domains.gambit;

import java.time.Instant;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * @author Steve Ebersole
 */
@Entity
public class SimpleEntity {
	private Integer id;
	private String someString;
	private Integer someInteger;
	private Long someLong;
	private Date someDate;
	private Instant someInstant;

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getSomeString() {
		return someString;
	}

	public void setSomeString(String someString) {
		this.someString = someString;
	}

	public Integer getSomeInteger() {
		return someInteger;
	}

	public void setSomeInteger(Integer someInteger) {
		this.someInteger = someInteger;
	}

	public Long getSomeLong() {
		return someLong;
	}

	public void setSomeLong(Long someLong) {
		this.someLong = someLong;
	}

	@Temporal( TemporalType.TIMESTAMP )
	public Date getSomeDate() {
		return someDate;
	}

	public void setSomeDate(Date someDate) {
		this.someDate = someDate;
	}

	public Instant getSomeInstant() {
		return someInstant;
	}

	public void setSomeInstant(Instant someInstant) {
		this.someInstant = someInstant;
	}
}
