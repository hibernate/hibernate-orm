/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.inheritance.joined;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;


@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "DTYPE", discriminatorType = DiscriminatorType.STRING, length = 80)
@DiscriminatorValue("EventInformationT")
public class EventInformation implements java.io.Serializable {


	protected String notificationId;

	@Id
	public String getNotificationId() {
		return notificationId;
	}

	public void setNotificationId(String value) {
		this.notificationId = value;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( "EventInformationT: id = " + getNotificationId() );
		return sb.toString();
	}

}
