/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tuple;

import org.hibernate.FetchMode;
import org.hibernate.engine.spi.CascadeStyle;

/**
 * @deprecated No direct replacement
 */
@Deprecated(forRemoval = true)
public interface NonIdentifierAttribute extends Attribute {
	boolean isLazy();

	boolean isInsertable();

	boolean isUpdateable();

	boolean isNullable();

	/**
	 * @deprecated Use {@link NonIdentifierAttribute#isDirtyCheckable()} instead
	 */
	@Deprecated
	boolean isDirtyCheckable(boolean hasUninitializedProperties);

	boolean isDirtyCheckable();

	boolean isVersionable();

	CascadeStyle getCascadeStyle();

	FetchMode getFetchMode();
}
