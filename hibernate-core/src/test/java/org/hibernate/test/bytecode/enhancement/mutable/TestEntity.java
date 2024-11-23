/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.mutable;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
public class TestEntity {
	@Id
	private Long id;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "t_date")
	private Date date;

	@Basic
	@Column(name = "TEXTS")
	@Convert(converter = MapStringConverter.class)
	private Map<String, String> texts = new HashMap<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Map<String, String> getTexts() {
		return texts;
	}

	public void setTexts(Map<String, String> texts) {
		this.texts = texts;
	}
}
