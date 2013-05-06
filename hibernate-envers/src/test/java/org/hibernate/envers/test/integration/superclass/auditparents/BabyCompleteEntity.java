package org.hibernate.envers.test.integration.superclass.auditparents;

import javax.persistence.Entity;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.entities.StrIntTestEntity;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited(auditParents = {MappedParentEntity.class})
public class BabyCompleteEntity extends ChildCompleteEntity {
	private String baby;

	public BabyCompleteEntity() {
	}

	public BabyCompleteEntity(
			Long id,
			String grandparent,
			String notAudited,
			String parent,
			String child,
			StrIntTestEntity relation,
			String baby) {
		super( id, grandparent, notAudited, parent, child, relation );
		this.baby = baby;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof BabyCompleteEntity) ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		BabyCompleteEntity that = (BabyCompleteEntity) o;

		if ( baby != null ? !baby.equals( that.baby ) : that.baby != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (baby != null ? baby.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "BabyCompleteEntity(" + super.toString() + ", baby = " + baby + ")";
	}

	public String getBaby() {
		return baby;
	}

	public void setBaby(String baby) {
		this.baby = baby;
	}
}
