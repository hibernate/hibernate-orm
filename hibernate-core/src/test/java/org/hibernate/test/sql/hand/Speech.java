/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: $
package org.hibernate.test.sql.hand;


/**
 * @author Emmanuel Bernard
 */
public class Speech implements SpeechInterface {
	private Integer id;
	private String name;
	private Double length;

	@Override
	public Integer getId() {
		return id;
	}

	@Override
	public void setId(Integer id) {
		this.id = id;
	}

	@Override
	public Double getLength() {
		return length;
	}

	@Override
	public void setLength(Double length) {
		this.length = length;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}
}
