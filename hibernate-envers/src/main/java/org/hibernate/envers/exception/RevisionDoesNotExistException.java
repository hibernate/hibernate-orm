/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.exception;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class RevisionDoesNotExistException extends AuditException {
	private static final long serialVersionUID = -6417768274074962282L;

	private final Number revision;
	private final Date date;
	private final LocalDateTime localDateTime;
	private final Instant instant;

	public RevisionDoesNotExistException(Number revision) {
		super( "Revision " + revision + " does not exist." );
		this.revision = revision;
		this.date = null;
		this.localDateTime = null;
		this.instant = null;
	}

	public RevisionDoesNotExistException(Date date) {
		super( "There is no revision before or at " + date + "." );
		this.date = date;
		this.revision = null;
		this.localDateTime = null;
		this.instant = null;
	}

	public RevisionDoesNotExistException(LocalDateTime localDateTime) {
		super( "There is no revision before or at " + localDateTime + "." );
		this.localDateTime = localDateTime;
		this.revision = null;
		this.date = null;
		this.instant = null;
	}

	public RevisionDoesNotExistException(Instant instant) {
		super( "There is no revision before or at " + instant + "." );
		this.instant = instant;
		this.revision = null;
		this.date = null;
		this.localDateTime = null;
	}

	public Number getRevision() {
		return revision;
	}

	public Date getDate() {
		return date;
	}

	public LocalDateTime getLocalDateTime() {
		return localDateTime;
	}

	public Instant getInstant() {
		return instant;
	}

}
