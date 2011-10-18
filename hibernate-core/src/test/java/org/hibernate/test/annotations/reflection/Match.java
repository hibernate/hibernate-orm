//$Id$
package org.hibernate.test.annotations.reflection;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;
import javax.persistence.Version;

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
