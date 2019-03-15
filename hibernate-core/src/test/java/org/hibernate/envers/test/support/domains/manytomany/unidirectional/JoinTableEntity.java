/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.manytomany.unidirectional;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;


/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited
public class JoinTableEntity implements Serializable {
	@Id
	@GeneratedValue
	private Long id;

	private String data;

	@ManyToMany
	@JoinTable(name = "test_join_table",
			   joinColumns = @JoinColumn(name = "assoc_id1"),
			   inverseJoinColumns = @JoinColumn(name = "assoc_id2")
	)
	private Set<StrTestEntity> references = new HashSet<>();

	public JoinTableEntity() {
	}

	public JoinTableEntity(String data) {
		this.data = data;
	}

	public JoinTableEntity(Long id, String data) {
		this.id = id;
		this.data = data;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Set<StrTestEntity> getReferences() {
		return references;
	}

	public void setReferences(Set<StrTestEntity> references) {
		this.references = references;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		JoinTableEntity that = (JoinTableEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( data, that.data );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, data );
	}

	@Override
	public String toString() {
		return "JoinTableEntity{" +
				"id=" + id +
				", data='" + data + '\'' +
				'}';
	}
}
