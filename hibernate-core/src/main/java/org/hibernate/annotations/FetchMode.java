/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

/**
 * Defines how the association should be fetched, compared to
 * {@link javax.persistence.FetchType} which defines when it should be fetched
 *
 * @author Emmanuel Bernard
 */
public enum FetchMode {
	/**
	 * Use a secondary select for each individual entity, collection, or join load.
	 */
	SELECT( org.hibernate.FetchMode.SELECT ),
	/**
	 * Use an outer join to load the related entities, collections or joins.
	 */
	JOIN( org.hibernate.FetchMode.JOIN ),
	/**
	 * Available for collections only.
	 *
	 * When accessing a non-initialized collection, this fetch mode will trigger
	 * loading all elements of all collections of the same role for all owners
	 * associated with the persistence context using a single secondary select.
	 */
	SUBSELECT( org.hibernate.FetchMode.SELECT );

	private final org.hibernate.FetchMode hibernateFetchMode;

	FetchMode(org.hibernate.FetchMode hibernateFetchMode) {
		this.hibernateFetchMode = hibernateFetchMode;
	}

	public org.hibernate.FetchMode getHibernateFetchMode() {
		return hibernateFetchMode;
	}
}
