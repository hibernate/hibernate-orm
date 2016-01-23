/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate.foreignkeys.crossschema;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Andrea Boriero
 */
@Entity
@Table(schema = "SCHEMA2")
public class SchemaTwoEntity {

	@Id
	private String id;

	@OneToMany
	@JoinColumn
	private Set<SchemaOneEntity> schemaOneEntities = new HashSet<SchemaOneEntity>();
}
