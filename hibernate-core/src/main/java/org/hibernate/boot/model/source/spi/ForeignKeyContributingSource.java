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
package org.hibernate.boot.model.source.spi;

/**
 * Additional contract for things which describe foreign keys.
 *
 * @author Steve Ebersole
 */
public interface ForeignKeyContributingSource {
	/**
	 * Retrieve the name of the foreign key as supplied by the user, or {@code null} if the user supplied none.
	 *
	 * @return The user supplied foreign key name.
	 */
	public String getExplicitForeignKeyName();

	/**
	 * Primarily exists to support JPA's {@code @ForeignKey(NO_CONSTRAINT)}.
	 *
	 * @return {@code true} if the FK constraint should be created, {@code false} if not.
	 */
	public boolean createForeignKeyConstraint();

	/**
	 * Is "cascade delete" enabled for the foreign key? In other words, if a record in the parent (referenced)
	 * table is deleted, should the corresponding records in the child table automatically be deleted?
	 *
	 * @return {@code true}, if the cascade delete is enabled; {@code false}, otherwise.
	 */
	public boolean isCascadeDeleteEnabled();
}
