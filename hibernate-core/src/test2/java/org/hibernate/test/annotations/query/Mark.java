/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.query;
import javax.persistence.ColumnResult;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;

/**
 * Example of scalar result (not working right now)
 *
 * @author Emmanuel Bernard
 */
@Entity
@NamedNativeQuery(name = "average", query = "select avg(m.value) from Mark m", resultSetMapping = "columnmapping")
@SqlResultSetMapping(
		name = "columnmapping",
		columns = @ColumnResult(name = "aver")
)
public class Mark {
	@Id
	@GeneratedValue
	public int id;
	public int value;
}
