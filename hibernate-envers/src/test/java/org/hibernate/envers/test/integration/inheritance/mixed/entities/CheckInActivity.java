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
