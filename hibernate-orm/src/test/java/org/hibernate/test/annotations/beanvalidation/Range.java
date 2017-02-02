/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.annotations.beanvalidation;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class Range {

	@Id
	@GeneratedValue
	private Long id;

	@org.hibernate.validator.constraints.Range(min = 2, max = 10)
    @Column(name = "`value`")
	private Integer value;

	private Range() {
	}

	public Range(Integer value) {
		this.value = value;
	}
}



