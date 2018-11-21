/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.support.domains.helpdesk;

import java.time.Instant;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Steve Ebersole
 */
@Entity
public class Incident {
	private Integer id;

	private Instant reported;

	private Instant effectiveStart;
	private Instant effectiveEnd;

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
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
