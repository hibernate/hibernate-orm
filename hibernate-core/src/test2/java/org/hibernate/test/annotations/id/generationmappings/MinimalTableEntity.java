/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.id.generationmappings;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Entity
@Table( name = "MINIMAL_TBL" )
public class MinimalTableEntity {
	public static final String TBL_NAME = "minimal_tbl";

	private Long id;

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "MINIMAL_TBL")
	@TableGenerator( name = "MINIMAL_TBL", table = TBL_NAME )
	public Long getId() {
		return id;
	}

	public void setId(Long long1) {
		id = long1;
	}
}
