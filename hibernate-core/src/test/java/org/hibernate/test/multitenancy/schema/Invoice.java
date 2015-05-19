/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.multitenancy.schema;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.id.enhanced.TableGenerator;

/**
 * @author Steve Ebersole
 */
@Entity
public class Invoice {
	@Id
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "number_sequence")
	@GenericGenerator(
			name = "number_sequence",
			strategy = "org.hibernate.id.enhanced.TableGenerator",
			parameters = {
					@Parameter(name = TableGenerator.SEGMENT_VALUE_PARAM, value = "customer"),
					@Parameter(name = TableGenerator.INCREMENT_PARAM, value = "5"),
					@Parameter(name = TableGenerator.OPT_PARAM, value = "pooled")
			}
	)
	private Long id;

	public Long getId() {
		return id;
	}
}
