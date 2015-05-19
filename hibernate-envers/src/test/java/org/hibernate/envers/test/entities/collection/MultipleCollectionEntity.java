/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.collection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Version;

import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;

@Entity
@Audited
public class MultipleCollectionEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID", length = 10)
	private Long id;

	@Version
	@Column(name = "VERSION", nullable = false)
	private Integer version;

	@Column(name = "TEXT", length = 50, nullable = false)
	private String text;

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "MCE_ID", nullable = false)
	@AuditJoinTable(name = "MCE_RE1_AUD", inverseJoinColumns = @JoinColumn(name = "RE1_ID"))
	private List<MultipleCollectionRefEntity1> refEntities1 = new ArrayList<MultipleCollectionRefEntity1>();

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "MCE_ID", nullable = false)
	@AuditJoinTable(name = "MCE_RE2_AUD", inverseJoinColumns = @JoinColumn(name = "RE2_ID"))
	private List<MultipleCollectionRefEntity2> refEntities2 = new ArrayList<MultipleCollectionRefEntity2>();

	public Long getId() {
		return id;
	}

	public Integer getVersion() {
		return version;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public List<MultipleCollectionRefEntity1> getRefEntities1() {
		return Collections.unmodifiableList( refEntities1 );
	}

	public void addRefEntity1(MultipleCollectionRefEntity1 refEntity1) {
		refEntities1.add( refEntity1 );
	}

	public void removeRefEntity1(MultipleCollectionRefEntity1 refEntity1) {
		refEntities1.remove( refEntity1 );
	}

	public List<MultipleCollectionRefEntity2> getRefEntities2() {
		return Collections.unmodifiableList( refEntities2 );
	}

	public void addRefEntity2(MultipleCollectionRefEntity2 refEntity2) {
		refEntities2.add( refEntity2 );
	}

	public void removeRefEntity2(MultipleCollectionRefEntity2 refEntity2) {
		refEntities2.remove( refEntity2 );
	}

	/**
	 * For test purpose only.
	 */
	public void setRefEntities1(List<MultipleCollectionRefEntity1> refEntities1) {
		this.refEntities1 = refEntities1;
	}

	/**
	 * For test purpose only.
	 */
	public void setRefEntities2(List<MultipleCollectionRefEntity2> refEntities2) {
		this.refEntities2 = refEntities2;
	}

	@Override
	public String toString() {
		return "MultipleCollectionEntity [id=" + id + ", text=" + text
				+ ", refEntities1=" + refEntities1 + ", refEntities2="
				+ refEntities2 + "]";
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof MultipleCollectionEntity) ) {
			return false;
		}

		MultipleCollectionEntity that = (MultipleCollectionEntity) o;

		if ( refEntities1 != null ? !refEntities1.equals( that.refEntities1 ) : that.refEntities1 != null ) {
			return false;
		}
		if ( refEntities2 != null ? !refEntities2.equals( that.refEntities2 ) : that.refEntities2 != null ) {
			return false;
		}
		if ( text != null ? !text.equals( that.text ) : that.text != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = text != null ? text.hashCode() : 0;
		result = 31 * result + (refEntities1 != null ? refEntities1.hashCode() : 0);
		result = 31 * result + (refEntities2 != null ? refEntities2.hashCode() : 0);
		return result;
	}
}