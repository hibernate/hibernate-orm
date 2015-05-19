/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetoone.hhh4851;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;

@Entity
@DiscriminatorValue(value = "T")
public class Device extends Hardware {

	private ManagedDevice managedDevice;
	private String tag;

	public Device() {
	}

	@OneToOne(fetch = FetchType.LAZY, mappedBy = "device")
	public ManagedDevice getManagedDevice() {
		return managedDevice;
	}

	@Column(unique = true, nullable = true)
	public String getTag() {
		return tag;
	}

	public void setManagedDevice(ManagedDevice logicalterminal) {
		this.managedDevice = logicalterminal;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

}
