/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.exception;

import java.util.Date;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class RevisionDoesNotExistException extends AuditException {
	private static final long serialVersionUID = -6417768274074962282L;

	private final Number revision;
	private final Date date;

	public RevisionDoesNotExistException(Number revision) {
		super( "Revision " + revision + " does not exist." );
		this.revision = revision;
		this.date = null;
	}

	public RevisionDoesNotExistException(Date date) {
		super( "There is no revision before or at " + date + "." );
		this.date = date;
		this.revision = null;
	}

	public Number getRevision() {
		return revision;
	}

	public Date getDate() {
		return date;
	}
}
