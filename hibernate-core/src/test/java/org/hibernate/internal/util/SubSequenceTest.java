/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SubSequenceTest {
	@Test
	public void subSequenceAllowsEmptyAtEnd() {
		CharSequence sequence = new SubSequence("abc", 0, 3);
		CharSequence empty = sequence.subSequence(3, 3);

		assertThat(empty.toString()).isEmpty();
		assertThat(empty.length()).isZero();
	}

	@Test
	public void subSequenceRejectsEndBeforeStart() {
		CharSequence sequence = new SubSequence("abc", 0, 3);

		assertThatThrownBy(() -> sequence.subSequence(2, 1))
				.isInstanceOf(StringIndexOutOfBoundsException.class);
	}
}
