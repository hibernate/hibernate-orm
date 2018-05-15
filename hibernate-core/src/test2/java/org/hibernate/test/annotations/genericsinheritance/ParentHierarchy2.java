/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.genericsinheritance;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public class ParentHierarchy2<C extends ChildHierarchy2> extends Parent<C> {

}
