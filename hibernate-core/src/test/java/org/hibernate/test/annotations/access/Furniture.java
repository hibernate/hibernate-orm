/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.access;
import javax.persistence.Entity;
import org.hibernate.annotations.AccessType;

/**
 * @author Emmanuel Bernard
 */
@Entity
@AccessType("field")
public class Furniture extends BaseFurniture {

}
