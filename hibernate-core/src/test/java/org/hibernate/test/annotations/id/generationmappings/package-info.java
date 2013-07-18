@GenericGenerator(name = "SequencePerEntityGenerator",
		strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
		parameters = {
				@Parameter(name = "prefer_sequence_per_entity", value = "true"),
				@Parameter(name = "sequence_per_entity_suffix", value = DedicatedSequenceEntity1.SEQUENCE_SUFFIX)
		}

)
package org.hibernate.test.annotations.id.generationmappings;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
