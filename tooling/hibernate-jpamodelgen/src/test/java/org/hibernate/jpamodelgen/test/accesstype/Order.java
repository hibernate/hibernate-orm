/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.accesstype;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
//@Entity
public class Order {
	
	//@Id
	long id;
	
	//@OneToMany
	Set<Item> items;
	
	boolean filled;
	Date date;
	
	//@OneToMany
	List<String> notes;
	
	//@ManyToOne
	Shop shop;
}
