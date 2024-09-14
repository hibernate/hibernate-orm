/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bootstrap.binding.hbm.cid.nonaggregated.dynamic;

/**
 * Entity used as target for a key-many-to-one
 *
 * @author Steve Ebersole
 */
public class ChangeGroup {
	private Integer id;
	private String name;

	/**
	 * For persistence
	 */
	@SuppressWarnings("unused")
	private ChangeGroup() {
	}

	public ChangeGroup(String name) {
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
