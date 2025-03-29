/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
@GenericGenerator(name = "SequencePerEntityGenerator",
		strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
		parameters = {
				@Parameter(name = "prefer_sequence_per_entity", value = "true"),
				@Parameter(name = "sequence_per_entity_suffix", value = DedicatedSequenceEntity1.SEQUENCE_SUFFIX)
		}

)
package org.hibernate.orm.test.annotations.id.generationmappings.sub;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
