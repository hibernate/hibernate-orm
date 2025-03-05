/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.mixed;

import jakarta.persistence.*;

@Entity
public class AnnotationEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "annotationentity_id_seq")
	@SequenceGenerator(
			name = "annotationentity_id_seq",
			sequenceName = "annotationentity_id_seq"
	)
	private Long _id;

	/**
	 * Get the identifier.
	 *
	 * @return the id.
	 */
	public Long getId()
	{
		return _id;
	}
}
