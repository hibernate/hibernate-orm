/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.join;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;

/**
 * @author Emmanuel Bernard
 */
@Entity
@SecondaryTable(
		name = "ExtendedDeath",
		pkJoinColumns = @PrimaryKeyJoinColumn(name = "DEATH_ID")
)
public class Death implements Serializable {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Integer id;
	@Column(name = "death_date")
	public Date date;
	@Column(table = "ExtendedDeath")
	public String howDoesItHappen;
}
