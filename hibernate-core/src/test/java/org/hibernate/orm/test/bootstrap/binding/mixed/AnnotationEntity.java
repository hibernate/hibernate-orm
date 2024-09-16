/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
