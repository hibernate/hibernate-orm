/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.onetoone.jointable;

import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

/**
 * @author Christian Beikov
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class ABlockableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	@Column(name = "id")
	private Long id;

	// We have two one-to-one associations to make sure parent_id isn't considered as part of this table regarding duplicate mappings
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinTable(
			name = "TBL_QUEUE",
			joinColumns = @JoinColumn(name = "child_id", referencedColumnName = "id"),
			inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
	)
	private OtherEntity other;

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinTable(
			name = "TBL_QUEUE2",
			joinColumns = @JoinColumn(name = "child_id", referencedColumnName = "id"),
			inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
	)
	private ABlockableEntity other2;

	// Two many-to-ones to make sure that still works in this scenario
	@ManyToOne(fetch = FetchType.LAZY)
	private OtherEntity manyToOne1;

	@ManyToOne(fetch = FetchType.LAZY)
	private ABlockableEntity manyToOne2;

	// Two one-to-manys to make sure that considering one-to-one joins in the entity persister doesn't break this
	@OneToMany
	private Set<OtherEntity> oneToMany1;

	@OneToMany
	private Set<ABlockableEntity> oneToMany2;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public OtherEntity getOther() {
		return other;
	}

	public void setOther(OtherEntity other) {
		this.other = other;
	}

	public ABlockableEntity getOther2() {
		return other2;
	}

	public void setOther2(ABlockableEntity other2) {
		this.other2 = other2;
	}

	public OtherEntity getManyToOne1() {
		return manyToOne1;
	}

	public void setManyToOne1(OtherEntity manyToOne1) {
		this.manyToOne1 = manyToOne1;
	}

	public ABlockableEntity getManyToOne2() {
		return manyToOne2;
	}

	public void setManyToOne2(ABlockableEntity manyToOne2) {
		this.manyToOne2 = manyToOne2;
	}

	public Set<OtherEntity> getOneToMany1() {
		return oneToMany1;
	}

	public void setOneToMany1(Set<OtherEntity> oneToMany1) {
		this.oneToMany1 = oneToMany1;
	}

	public Set<ABlockableEntity> getOneToMany2() {
		return oneToMany2;
	}

	public void setOneToMany2(Set<ABlockableEntity> oneToMany2) {
		this.oneToMany2 = oneToMany2;
	}
}