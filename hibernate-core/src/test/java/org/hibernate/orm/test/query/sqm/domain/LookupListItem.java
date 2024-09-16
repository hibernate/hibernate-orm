/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sqm.domain;

/**
 * @author Steve Ebersole
 */
public interface LookupListItem {
	Integer getId();

	String getDisplayValue();
}
