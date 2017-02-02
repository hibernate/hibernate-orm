/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.reflection;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.IdClass;
import javax.persistence.SequenceGenerator;
import javax.persistence.TableGenerator;

/**
 * @author Emmanuel Bernard
 */
@Entity
@IdClass(SocialSecurityNumber.class)
@DiscriminatorValue("Moral")
@SequenceGenerator(name = "seq")
@TableGenerator(name = "table")
public class SocialSecurityMoralAccount {
	public String number;
	public String countryCode;
}
