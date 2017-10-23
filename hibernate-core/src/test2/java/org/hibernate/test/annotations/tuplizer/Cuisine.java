/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.tuplizer;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.Tuplizer;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Tuplizer(impl = DynamicEntityTuplizer.class)
public interface Cuisine {
	@Id
	@GeneratedValue
	public Long getId();
	public void setId(Long id);

	public String getName();
	public void setName(String name);

	@Tuplizer(impl = DynamicComponentTuplizer.class)
	public Country getCountry();
	public void setCountry(Country country);


}
