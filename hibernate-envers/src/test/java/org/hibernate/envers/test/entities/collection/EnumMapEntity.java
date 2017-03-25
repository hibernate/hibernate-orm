/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.collection;

import java.util.EnumMap;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
@Audited
public class EnumMapEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@OneToMany(cascade = CascadeType.ALL)
	private Map<EnumType, EnumMapType> types = new EnumMap<>(EnumType.class);

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Map<EnumType, EnumMapType> getTypes() {
		return types;
	}

	public void setTypes(Map<EnumType, EnumMapType> types) {
		this.types = types;
	}

	public enum EnumType {
		TYPE_A,
		TYPE_B,
		TYPE_C
	}
}
