/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.event.collection.detached;

import java.util.ArrayList;
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

@Entity
public class MultipleCollectionEntity implements org.hibernate.test.event.collection.Entity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID", length = 10)
	private Long id;

	@Column(name = "TEXT", length = 50, nullable = false)
	private String text;

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "MCE_ID", nullable = false)
	private List<MultipleCollectionRefEntity1> refEntities1 = new ArrayList<MultipleCollectionRefEntity1>();

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "MCE_ID", nullable = false)
	private List<MultipleCollectionRefEntity2> refEntities2 = new ArrayList<MultipleCollectionRefEntity2>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public List<MultipleCollectionRefEntity1> getRefEntities1() {
		return refEntities1;
	}

	public void setRefEntities1(List<MultipleCollectionRefEntity1> refEntities1) {
		this.refEntities1 = refEntities1;
	}

	public void addRefEntity1(MultipleCollectionRefEntity1 refEntity1) {
		refEntities1.add(refEntity1);
	}

	public void removeRefEntity1(MultipleCollectionRefEntity1 refEntity1) {
		refEntities1.remove(refEntity1);
	}

	public List<MultipleCollectionRefEntity2> getRefEntities2() {
		return refEntities2;
	}

	public void setRefEntities2(List<MultipleCollectionRefEntity2> refEntities2) {
		this.refEntities2 = refEntities2;
	}

	public void addRefEntity2(MultipleCollectionRefEntity2 refEntity2) {
		refEntities2.add(refEntity2);
	}

	public void removeRefEntity2(MultipleCollectionRefEntity2 refEntity2) {
		refEntities2.remove(refEntity2);
	}

	@Override
	public String toString() {
		return "MultipleCollectionEntity [id=" + id + ", text=" + text
				+ ", refEntities1=" + refEntities1 + ", refEntities2="
				+ refEntities2 + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		result = prime * result
				+ ((refEntities1 == null) ? 0 : refEntities1.hashCode());
		result = prime * result
				+ ((refEntities2 == null) ? 0 : refEntities2.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MultipleCollectionEntity other = (MultipleCollectionEntity) obj;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		if (refEntities1 == null) {
			if (other.refEntities1 != null)
				return false;
		} else if (!refEntities1.equals(other.refEntities1))
			return false;
		if (refEntities2 == null) {
			if (other.refEntities2 != null)
				return false;
		} else if (!refEntities2.equals(other.refEntities2))
			return false;
		return true;
	}

	
	public MultipleCollectionEntity deepCopy() {
		MultipleCollectionEntity clone = new MultipleCollectionEntity();
		clone.setText(this.text);
		clone.setId(this.id);
		
		for (MultipleCollectionRefEntity1 refEntity1 : refEntities1) {
			clone.addRefEntity1(refEntity1.deepCopy(clone));
		}
		for (MultipleCollectionRefEntity2 refEntity2 : refEntities2) {
			clone.addRefEntity2(refEntity2.deepCopy(clone));
		}
		return clone;
	}


}
