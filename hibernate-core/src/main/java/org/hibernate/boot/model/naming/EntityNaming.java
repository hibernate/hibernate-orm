/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.boot.model.naming;

/**
 * @author Steve Ebersole
 */
public interface EntityNaming {
	/**
	 * Retrieve the fully-qualified entity class name.  Note that for
	 * dynamic entities, this may return (what???).
	 *
	 * todo : what should this return for dynamic entities?  null?  The entity name?
	 *
	 * @return The entity class name.
	 */
	public String getClassName();

	/**
	 * The Hibernate entity name.  This might be either:<ul>
	 *     <li>The explicitly specified entity name, if one</li>
	 *     <li>The unqualified entity class name if no entity name was explicitly specified</li>
	 * </ul>
	 *
	 * @return The Hibernate entity name
	 */
	public String getEntityName();

	/**
	 * The JPA-specific entity name.  See {@link javax.persistence.Entity#name()} for details.
	 *
	 * @return The JPA entity name, if one was specified.  May return {@code null} if one
	 * was not explicitly specified.
	 */
	public String getJpaEntityName();
}
