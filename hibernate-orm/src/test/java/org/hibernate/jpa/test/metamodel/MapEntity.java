/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metamodel;

import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;

@Entity
@Table( name = "MAP_ENTITY" )
public class MapEntity {

	@Id
	@Column(name="key_")
	private String key;
	
	@ElementCollection(fetch=FetchType.LAZY)
	@CollectionTable(name="MAP_ENTITY_NAME", joinColumns=@JoinColumn(name="key_"))
	@MapKeyColumn(name="lang_")
	private Map<String, MapEntityLocal> localized;
	
	public String getKey() {
		return key;
	}
	
	public void setKey(String key) {
		this.key = key;
	}
}
