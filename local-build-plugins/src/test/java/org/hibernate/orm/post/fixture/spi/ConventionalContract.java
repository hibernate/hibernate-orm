/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post.fixture.spi;

import org.hibernate.orm.post.SpiJandexClassifierTests.DerivedCollaborator;

/// Exact-`.spi` convention fixture.
///
/// @author Steve Ebersole
public class ConventionalContract {
	public DerivedCollaborator state() {
		return null;
	}
}
