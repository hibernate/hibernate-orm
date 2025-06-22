/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.custom.declaredtype;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public interface IHeadList<X> extends List<X> {
	X head();
}
