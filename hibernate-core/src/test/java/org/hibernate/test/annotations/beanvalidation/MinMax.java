/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.annotations.beanvalidation;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Column;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class MinMax {

	@Id
	@GeneratedValue
	private Long id;

	@Max(10)
	@Min(2)
    @Column(name = "`value`")
	private Integer value;

	private MinMax() {
	}

	public MinMax(Integer value) {
		this.value = value;
	}
}



