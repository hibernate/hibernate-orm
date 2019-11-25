/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.milestone;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

@Entity
@Table(name = "EVNT_MILSTN")
@IdClass(EventMilestonePK.class)
public class EventMilestone {

	@Id
	@Column(name = "EVNT_ID")
	private Long eventId;

	@Id
	@Column(name = "EVNT_MILSTN_ID")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long eventMilestoneId;

	public EventMilestonePK getId() {
		return new EventMilestonePK( eventId, eventMilestoneId );
	}

	public void setId(EventMilestonePK id) {
		this.eventId = id.getEventId();
		this.eventMilestoneId = id.getEventMilestoneId();
	}
}
