/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.map;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;

@Entity
@Table(name = "multilingual")
public class MultilingualString {
	@Id
	@GeneratedValue
	private long id;

	@ElementCollection
	@MapKeyColumn(name = "language", insertable = false, updatable = false)
	@CollectionTable(name = "multilingual_string_map", joinColumns = @JoinColumn(name = "string_id"))
	private Map<String, LocalizedString> map = new HashMap<String, LocalizedString>();

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}

	public Map<String, LocalizedString> getMap() {
		return map;
	}
	public void setMap(Map<String, LocalizedString> map) {
		this.map = map;
	}
}
