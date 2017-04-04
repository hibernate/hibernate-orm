/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.entity;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@TypeDef(
		name = "phoneNumber",
		defaultForType = PhoneNumber.class,
		typeClass = PhoneNumberType.class
)

/**
 * @author Sharath Reddy
 *
 */
@Entity
public class ContactDetails {

	@Id
	@GeneratedValue
	private int id;
		
	private PhoneNumber localPhoneNumber;
	@Type(type="phoneNumber")
	private OverseasPhoneNumber overseasPhoneNumber;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public PhoneNumber getLocalPhoneNumber() {
		return localPhoneNumber;
	}
	public void setLocalPhoneNumber(PhoneNumber localPhoneNumber) {
		this.localPhoneNumber = localPhoneNumber;
	}
	public OverseasPhoneNumber getOverseasPhoneNumber() {
		return overseasPhoneNumber;
	}
	public void setOverseasPhoneNumber(OverseasPhoneNumber overseasPhoneNumber) {
		this.overseasPhoneNumber = overseasPhoneNumber;
	}
	
}
