/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.annotations;

/**
 * Cascade types (can override default JPA cascades).
 */
public enum CascadeType {
	/**
	 * Includes all types listed here.
	 */
	ALL,
	/**
	 * Corresponds to {@link javax.persistence.CascadeType#PERSIST}.
	 */
	PERSIST,
	/**
	 * Corresponds to {@link javax.persistence.CascadeType#MERGE}.
	 */
	MERGE,
	/**
	 * Corresponds to {@link javax.persistence.CascadeType#REMOVE}.
	 */
	REMOVE,
	/**
	 * Corresponds to {@link javax.persistence.CascadeType#REFRESH}.
	 */
	REFRESH,
	/**
	 * Corresponds to the Hibernate native DELETE action.
	 */
	DELETE,
	/**
	 * Corresponds to the Hibernate native SAVE_UPDATE (direct reattachment) action.
	 */
	SAVE_UPDATE,
	/**
	 * Corresponds to the Hibernate native REPLICATE action.
	 */
	REPLICATE,
	/**
	 * Corresponds to the Hibernate native LOCK action.
	 */
	LOCK,
	/**
	 * Corresponds to {@link javax.persistence.CascadeType#REFRESH}.
	 */
	DETACH
}
