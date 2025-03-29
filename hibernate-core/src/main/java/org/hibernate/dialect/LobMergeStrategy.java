/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

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
	Blob mergeBlob(Blob original, Blob target, SharedSessionContractImplementor session);

	/**
	 * Perform merge on {@link Clob} values.
	 *
	 * @param original The detached {@code CLOB} state
	 * @param target The managed {@code CLOB} state
	 * @param session The session
	 *
	 * @return The merged {@code CLOB} state
	 */
	Clob mergeClob(Clob original, Clob target, SharedSessionContractImplementor session);

	/**
	 * Perform merge on {@link NClob} values.
	 *
	 * @param original The detached {@code NCLOB} state
	 * @param target The managed {@code NCLOB} state
	 * @param session The session
	 *
	 * @return The merged {@code NCLOB} state
	 */
	NClob mergeNClob(NClob original, NClob target, SharedSessionContractImplementor session);
}
