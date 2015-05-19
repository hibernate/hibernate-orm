/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
