/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.namingstrategy.complete;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.NaturalId;

/**
 * @author Steve Ebersole
 */
@Entity
public class ZipCode {
	private Integer id;
	private String code;

	private String city;
	private State state;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Basic
	@NaturalId
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@Basic
	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	@Enumerated
	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}
}
