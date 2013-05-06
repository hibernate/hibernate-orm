/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers;


/**
 * An implementation of this class, having a no-arg constructor, should be passed as an argument to the
 * {@link RevisionEntity} annotation.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public interface RevisionListener {
	/**
	 * Called when a new revision is created.
	 *
	 * @param revisionEntity An instance of the entity annotated with {@link RevisionEntity}, which will be persisted
	 * after this method returns. All properties on this entity that are to be persisted should be set by this method.
	 */
	void newRevision(Object revisionEntity);
}
