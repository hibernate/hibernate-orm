/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.inheritance.mixed.entities;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.envers.Audited;

@Audited
@Entity
@DiscriminatorValue(value = "CHECK_IN")
public class CheckInActivity extends AbstractCheckActivity {

	@Override
	public boolean equals(Object obj) {
		if ( obj == this ) {
			return true;
		}
		if ( !(obj instanceof CheckInActivity) ) {
			return false;
		}
		CheckInActivity checkInActivity = (CheckInActivity) obj;
		return getId().equals( checkInActivity.getId() );
	}

	@Override
	public int hashCode() {
		return getId().hashCode();
	}

}
