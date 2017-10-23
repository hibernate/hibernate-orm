/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: BasicHibernateAnnotationsTest.java 17531 2009-09-22 17:43:48Z epbernard $

package org.hibernate.test.annotations.query;
import javax.persistence.MappedSuperclass;

@org.hibernate.annotations.NamedQuery(
		name = "night.olderThan", 
		query = "select n from Night n where n.date <= :date"
)

@MappedSuperclass
public class Darkness {

}
