/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.helpdesk;

import java.time.Instant;

import javax.persistence.ColumnResult;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.SqlResultSetMapping;

/**
 * @author Steve Ebersole
 */
@Entity
@SqlResultSetMapping(
		name = "incident_summary",
		columns = {
				@ColumnResult( name = "id" ),
				@ColumnResult( name = "description" ),
				@ColumnResult( name = "reported", type = Instant.class )
		}
)
public class Incident {
	private Integer id;
	private String description;

	private Instant reported;

	private Instant effectiveStart;
	private Instant effectiveEnd;

	public Incident() {
	}

	public Incident(Integer id, String description, Instant reported) {
		this.id = id;
		this.description = description;
		this.reported = reported;
	}

	public Incident(
			Integer id,
			String description,
			Instant reported,
			Instant effectiveStart,
			Instant effectiveEnd) {
		this.id = id;
		this.description = description;
		this.reported = reported;
		this.effectiveStart = effectiveStart;
		this.effectiveEnd = effectiveEnd;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Instant getReported() {
		return reported;
	}

	public void setReported(Instant reported) {
		this.reported = reported;
	}

	public Instant getEffectiveStart() {
		return effectiveStart;
	}

	public void setEffectiveStart(Instant effectiveStart) {
		this.effectiveStart = effectiveStart;
	}

	public Instant getEffectiveEnd() {
		return effectiveEnd;
	}

	public void setEffectiveEnd(Instant effectiveEnd) {
		this.effectiveEnd = effectiveEnd;
	}
}
