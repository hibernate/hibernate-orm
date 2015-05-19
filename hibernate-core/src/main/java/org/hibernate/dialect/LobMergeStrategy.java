/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
