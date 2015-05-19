/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetoone.hhh4851;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * A group of {@link LogicalTerminal logical terminals}. Used to group them for Configuration purpose. That's why a
 * LogicalTerminal can only have one TerminalGroup.
 */
@Entity
@Table
public class DeviceGroupConfig extends BaseEntity {

	private String name = null;

	public DeviceGroupConfig() {

	}

	/**
	 * Not unique, because we could use the same name in two different organizations.
	 *
	 * @return
	 */
	@Column(nullable = false)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
