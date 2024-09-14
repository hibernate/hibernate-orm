/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.legacy;


/**
 * @author Gavin King
 */
public class Part {

	private Long id;
	private String description;

	public String getDescription() {
		return description;
	}

	public Long getId() {
		return id;
	}

	public void setDescription(String string) {
		description = string;
	}

	public void setId(Long long1) {
		id = long1;
	}

	public static class SpecialPart extends Part {}

}
