/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.produce.domain;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class ConstructedLookupListItem implements LookupListItem {
	private final Integer id;
	private final String displayValue;

	public ConstructedLookupListItem(Integer id, String displayValue) {
		this.id = id;
		this.displayValue = displayValue;
	}

	@Override
	public Integer getId() {
		return id;
	}

	@Override
	public String getDisplayValue() {
		return displayValue;
	}
}
