/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.jpa.test.emops;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Race {
	@Id @GeneratedValue public Integer id;
	@OrderColumn( name="index_" ) @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@org.hibernate.annotations.Cascade( { org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN }) 
	public List<Competitor> competitors = new ArrayList<Competitor>();
	public String name;
}
