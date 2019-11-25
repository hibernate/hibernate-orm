/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.milestone;

import java.io.Serializable;
import java.util.Objects;

public class EventMilestonePK implements Serializable {

	private Long eventId;
	private Long eventMilestoneId;

	public EventMilestonePK(Long eventId, Long eventMilestoneId) {
		this.eventId = eventId;
		this.eventMilestoneId = eventMilestoneId;
	}

	public EventMilestonePK(Long eventId) {
		this.eventId = eventId;
	}

	private EventMilestonePK() {
	}

	public Long getEventId() {
		return eventId;
	}

	public Long getEventMilestoneId() {
		return eventMilestoneId;
	}

	public void setEventMilestoneId(Long eventMilestoneId) {
		this.eventMilestoneId = eventMilestoneId;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		EventMilestonePK that = (EventMilestonePK) o;
		return Objects.equals( eventId, that.eventId ) &&
				Objects.equals( eventMilestoneId, that.eventMilestoneId );
	}

	@Override
	public int hashCode() {
		return Objects.hash( eventId, eventMilestoneId );
	}
}
