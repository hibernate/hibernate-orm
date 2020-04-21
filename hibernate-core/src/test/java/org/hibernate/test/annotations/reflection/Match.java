/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.reflection;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "matchtable", schema = "matchschema")
@SecondaryTable(name = "extendedMatch")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@NamedQueries({
@NamedQuery(name = "matchbyid", query = "select m from Match m where m.id = :id"),
@NamedQuery(name = "getAllMatches2", query = "select m from Match m")
		})
@NamedNativeQueries({
@NamedNativeQuery(name = "matchbyid", query = "select m from Match m where m.id = :id", resultSetMapping = "matchrs"),
@NamedNativeQuery(name = "getAllMatches2", query = "select m from Match m", resultSetMapping = "matchrs")
		})
public class Match extends Competition {
	public String competitor1Point;
	@Version
	public Integer version;
	public SocialSecurityNumber playerASSN;
}
