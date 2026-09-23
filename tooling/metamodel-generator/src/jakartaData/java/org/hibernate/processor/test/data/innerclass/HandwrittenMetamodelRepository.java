/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.innerclass;

import java.util.List;

import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Select;

@Repository
public interface HandwrittenMetamodelRepository {

	@Find(HandwrittenEntity.class)
	@Select(_HandwrittenEntity.HANDWRITTEN_NAME)
	List<String> names();
}
