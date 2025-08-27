/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
import jakarta.persistence.Embeddable;

@Embeddable
class TheEmbeddable {
	var valueOne: String? = null
	var valueTwo: String? = null
}
