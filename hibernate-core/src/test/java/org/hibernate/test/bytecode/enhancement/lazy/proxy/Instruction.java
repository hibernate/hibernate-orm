/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity(name = "Instruction")
@Table(name = "instruction")
public class Instruction extends BaseEntity {
	private String summary;

	/**
	 * Used by Hibernate
	 */
	@SuppressWarnings("unused")
	public Instruction() {
		super();
	}

	@SuppressWarnings("WeakerAccess")
	public Instruction(Integer id, String summary) {
		super( id );
		this.summary = summary;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}
}
