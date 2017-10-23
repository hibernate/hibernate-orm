/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.id.generationmappings;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Entity
public class CompleteSequenceEntity {
	public static final String SEQ_NAME = "some_other_sequence";
	private Long id;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "COMPLETE_SEQ")
	@javax.persistence.SequenceGenerator(
			name = "COMPLETE_SEQ",
			sequenceName = SEQ_NAME,
			initialValue = 1000,
			allocationSize = 52,
			catalog = "my_catalog",
			schema = "my_schema"
	)
	public Long getId() {
		return id;
	}

	public void setId(Long long1) {
		id = long1;
	}
}
