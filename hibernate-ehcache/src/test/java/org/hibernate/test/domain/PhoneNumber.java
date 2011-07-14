/**
 *  Copyright 2003-2010 Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.hibernate.test.domain;

import java.io.Serializable;

/**
 * PhoneNumber
 */
public class PhoneNumber implements Serializable {
	private long personId = 0;
	private String numberType = "home";
	private long phone = 0;

	public long getPersonId() {
		return personId;
	}

	public void setPersonId(long personId) {
		this.personId = personId;
	}

	public String getNumberType() {
		return numberType;
	}

	public void setNumberType(String numberType) {
		this.numberType = numberType;
	}

	public long getPhone() {
		return phone;
	}

	public void setPhone(long phone) {
		this.phone = phone;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
						 + ((numberType == null) ? 0 : numberType.hashCode());
		result = prime * result + (int)(personId ^ (personId >>> 32));
		result = prime * result + (int)(phone ^ (phone >>> 32));
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final PhoneNumber other = (PhoneNumber)obj;
		if (numberType == null) {
			if (other.numberType != null)
				return false;
		} else if (!numberType.equals(other.numberType))
			return false;
		if (personId != other.personId)
			return false;
		if (phone != other.phone)
			return false;
		return true;
	}

	public String toString() {
		return numberType + ":" + phone;
	}

}
