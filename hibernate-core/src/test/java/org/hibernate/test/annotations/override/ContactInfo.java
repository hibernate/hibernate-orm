/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.override;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;


@Embeddable
public class ContactInfo {
	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name="address_id_fk")
	Addr address;

	@ManyToMany(cascade = CascadeType.ALL)
	List<PhoneNumber> phoneNumbers;

	@Embedded
	SocialTouchPoints social;

	public Addr getAddress() {
		return address;
	}

	public void setAddr(Addr address) {
		this.address = address;
	}

	public List<PhoneNumber> getPhoneNumbers() {
		return phoneNumbers;
	}

	public void setPhoneNumbers(List<PhoneNumber> phoneNumbers) {
		this.phoneNumbers = phoneNumbers;
	}

	public SocialTouchPoints getSocial() {
		return social;
	}

	public void setSocial(SocialTouchPoints social) {
		this.social = social;
	}

}
