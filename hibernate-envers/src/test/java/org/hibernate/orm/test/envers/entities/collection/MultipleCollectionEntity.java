/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.collection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;

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
		return refEntities1;
	}

	public void addRefEntity1(MultipleCollectionRefEntity1 refEntity1) {
		refEntities1.add( refEntity1 );
	}

	public void removeRefEntity1(MultipleCollectionRefEntity1 refEntity1) {
		refEntities1.remove( refEntity1 );
	}

	public List<MultipleCollectionRefEntity2> getRefEntities2() {
		return refEntities2;
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
	public int hashCode() {
		return Objects.hash( id );
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;
		if ( getClass() != obj.getClass() )
			return false;
		MultipleCollectionEntity other = (MultipleCollectionEntity) obj;
		return Objects.equals( id, other.id );
	}

}
