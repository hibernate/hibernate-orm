/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.e2.a;
import java.io.Serializable;
import javax.persistence.Embedded;

/**
 * @author Emmanuel Bernard
 */
public class DependentId implements Serializable {
	String name; // matches name of @Id attribute
	@Embedded
	EmployeeId emp; //matches name of attribute and type of Employee PK
}
