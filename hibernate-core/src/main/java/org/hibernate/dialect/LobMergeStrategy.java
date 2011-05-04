/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.dialect;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;

import org.hibernate.engine.spi.SessionImplementor;

/**
 * Strategy for how dialects need {@code LOB} values to be merged.
 *
 * @author Steve Ebersole
 */
public interface LobMergeStrategy {
	/**
	 * Perform merge on {@link Blob} values.
	 *
	 * @param original The detached {@code BLOB} state
	 * @param target The managed {@code BLOB} state
	 * @param session The session
	 *
	 * @return The merged {@code BLOB} state
	 */
	public Blob mergeBlob(Blob original, Blob target, SessionImplementor session);

	/**
	 * Perform merge on {@link Clob} values.
	 *
	 * @param original The detached {@code CLOB} state
	 * @param target The managed {@code CLOB} state
	 * @param session The session
	 *
	 * @return The merged {@code CLOB} state
	 */
	public Clob mergeClob(Clob original, Clob target, SessionImplementor session);

	/**
	 * Perform merge on {@link NClob} values.
	 *
	 * @param original The detached {@code NCLOB} state
	 * @param target The managed {@code NCLOB} state
	 * @param session The session
	 *
	 * @return The merged {@code NCLOB} state
	 */
	public NClob mergeNClob(NClob original, NClob target, SessionImplementor session);
}
