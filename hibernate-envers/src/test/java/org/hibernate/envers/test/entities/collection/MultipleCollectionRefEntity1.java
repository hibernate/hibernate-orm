package org.hibernate.envers.test.entities.collection;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Version;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

@Entity
@Audited
public class MultipleCollectionRefEntity1 {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID", length = 10)
	private Long id;

	@Version
	@Column(name = "VERSION", nullable = false)
	private Integer version;

	@Column(name = "TEXT", length = 50, nullable = false)
	private String text;

	@ManyToOne
	@JoinColumn(name = "MCE_ID", nullable = false, insertable = false, updatable = false, foreignKey = @ForeignKey(name = "FK_RE1_MCE") )
	@NotAudited
	private MultipleCollectionEntity multipleCollectionEntity;

	@Column(name = "MCE_ID", insertable = false, updatable = false)
	@NotAudited
	private Long multipleCollectionEntityId;

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

	public MultipleCollectionEntity getMultipleCollectionEntity() {
		return multipleCollectionEntity;
	}

	public void setMultipleCollectionEntity(MultipleCollectionEntity multipleCollectionEntity) {
		this.multipleCollectionEntity = multipleCollectionEntity;
	}

	public Long getMultipleCollectionEntityId() {
		return multipleCollectionEntityId;
	}

	public void setMultipleCollectionEntityId(Long multipleCollectionEntityId) {
		this.multipleCollectionEntityId = multipleCollectionEntityId;
	}

	@Override
	public String toString() {
		return "MultipleCollectionRefEntity1 [id=" + id + ", text=" + text
				+ ", multipleCollectionEntityId=" + multipleCollectionEntityId
				+ "]";
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof MultipleCollectionRefEntity1) ) {
			return false;
		}

		MultipleCollectionRefEntity1 that = (MultipleCollectionRefEntity1) o;

		if ( text != null ? !text.equals( that.text ) : that.text != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return text != null ? text.hashCode() : 0;
	}
}