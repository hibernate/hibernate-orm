/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.mappedsuperclass.dao;

import org.hibernate.annotations.processing.Find;

public interface Queries {
	@Find
	Child getChild(Long id);
}
