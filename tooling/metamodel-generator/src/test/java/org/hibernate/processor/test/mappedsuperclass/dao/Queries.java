/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.mappedsuperclass.dao;

import org.hibernate.annotations.processing.Find;

public interface Queries {
	@Find
	Child getChild(Long id);
}
