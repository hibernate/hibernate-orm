/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$
package org.hibernate.test.annotations.idmanytoone.alphabetical;
import java.math.BigInteger;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class Benefserv {
	@Id
	private BigInteger idpk;

	@ManyToOne
	private Service idpkser;
}
