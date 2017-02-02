/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetoone.hhh4851;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(discriminatorType = DiscriminatorType.STRING, name = "DeviceType", length = 1)
@DiscriminatorValue(value = "C")
public class Hardware extends BaseEntity {

	private Hardware parent = null;

	protected Hardware() {

	}

	public Hardware(Hardware parent) {
		this.parent = parent;

	}

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	public Hardware getParent() {
		return this.parent;
	}

	public void setParent(Hardware parent) {
		this.parent = parent;
	}

}
