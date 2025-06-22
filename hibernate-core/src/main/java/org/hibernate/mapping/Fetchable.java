/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;
import org.hibernate.FetchMode;

/**
 * Any mapping with an outer-join attribute
 *
 * @author Gavin King
 */
public interface Fetchable {
	FetchMode getFetchMode();
	void setFetchMode(FetchMode joinedFetch);
	boolean isLazy();
	void setLazy(boolean lazy);
}
