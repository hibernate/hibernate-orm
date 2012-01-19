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
package org.hibernate;

/**
 * Possible values regarding the mutability of a natural id.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.annotations.NaturalId
 */
public enum NaturalIdMutability {
	/**
	 * The natural id is mutable.  Hibernate will write changes in the natural id value when flushing updates to the
	 * the entity to the database.  Also, it will invalidate any caching when such a change is detected.
	 */
	MUTABLE,

	/**
	 * The natural id is immutable.  Hibernate will ignore any changes in the natural id value when flushing updates
	 * to the entity to the database.  Additionally Hibernate <b>will not</b> check with the database to check if the
	 * natural id values change there.  Essentially the user is assuring Hibernate that the values will not change.
	 */
	IMMUTABLE,

	/**
	 * The natural id is immutable.  Hibernate will ignore any changes in the natural id value when flushing updates
	 * to the entity to the database.  However, Hibernate <b>will</b> check with the database to check if the natural
	 * id values change there.  This will ensure caching gets invalidated if the natural id value is changed in the
	 * database (outside of this Hibernate SessionFactory).
	 *
	 * Note however that frequently changing natural ids are really not natural ids and should really not be mapped
	 * as such.  The overhead of maintaining caching of natural ids in these cases is far greater than the benefit
	 * from such caching.  In such cases, a database index is a much better solution.
	 */
	IMMUTABLE_CHECKED,

	/**
	 * @deprecated Added in deprecated form solely to allow seamless working until the deprecated attribute
	 * {@link org.hibernate.annotations.NaturalId#mutable()} can be removed.
	 */
	@Deprecated
	UNSPECIFIED
}
