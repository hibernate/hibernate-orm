/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.spi.source;

/**
 * Provides the strategy for "unsaved" identifier and version property values
 * that indicates an instance is newly instantiated (unsaved), distinguishing
 * it from detached instances that were saved or loaded in a previous session.
 *
 * @author Gail Badner
 */
public interface UnsavedValueStrategy {

	/**
	 * Determines the strategy for an "unsaved" entity identifier value.
	 *
	 * @param identifierSource - the entity ID source.
	 * @param isIdAssigned - true, if the ID is assigned by the application;
	 *                       false, otherwise.
	 * @return the strategy for an "unsaved" entity identifier value;
	 */
	public String getIdUnsavedValue(IdentifierSource identifierSource, boolean isIdAssigned);

	/**
	 * Determines the strategy for an "unsaved" version or timestamp value.
	 *
	 * @param versionAttributeSource - the version source.
	 * @return the strategy for an "unsaved" version value.
	 */
	public String getVersionUnsavedValue(VersionAttributeSource versionAttributeSource);
}
