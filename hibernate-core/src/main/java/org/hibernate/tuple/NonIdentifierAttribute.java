/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tuple;

import org.hibernate.FetchMode;
import org.hibernate.annotations.OnDeleteAction;
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

	OnDeleteAction getOnDeleteAction();

	FetchMode getFetchMode();
}
