/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.interceptorbinding;

import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;
import org.hibernate.processor.test.data.interceptorbinding.binding.Audited;

@Repository
@Audited(
		mode = Audited.Mode.STRICT,
		entity = InterceptedBook.class,
		arrayType = InterceptedBook[].class,
		nested = @Audited.Nested(InterceptedBook.class),
		nestedArray = @Audited.Nested(InterceptedBook.class)
)
public interface InterceptedBookRepository {
	@Find
	@Audited(
			mode = Audited.Mode.STRICT,
			entity = InterceptedBook.class,
			arrayType = InterceptedBook[].class,
			nested = @Audited.Nested(InterceptedBook.class),
			nestedArray = @Audited.Nested(InterceptedBook.class)
	)
	InterceptedBook find(String isbn);
}
