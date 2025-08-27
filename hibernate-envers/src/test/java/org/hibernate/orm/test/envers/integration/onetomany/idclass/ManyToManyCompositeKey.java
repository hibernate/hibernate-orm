/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany.idclass;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

/**
 * @author Chris Cranford
 */
@Audited
@Entity
@IdClass(ManyToManyCompositeKey.ManyToManyId.class)
public class ManyToManyCompositeKey {
	@Id
	@ManyToOne(fetch = FetchType.EAGER)
	private OneToManyOwned oneToMany;

	@Id
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@ManyToOne(fetch = FetchType.EAGER)
	private ManyToOneOwned manyToOne;

	public ManyToManyCompositeKey() {

	}

	public ManyToManyCompositeKey(OneToManyOwned oneToMany, ManyToOneOwned manyToOne) {
		this.oneToMany = oneToMany;
		this.manyToOne = manyToOne;
	}

	public OneToManyOwned getOneToMany() {
		return this.oneToMany;
	}

	public ManyToOneOwned getManyToOne() {
		return this.manyToOne;
	}

	public ManyToManyId getId() {
		return new ManyToManyId( oneToMany, manyToOne );
	}

	@Override
	public int hashCode() {
		int result = 0;
		result = 31 * result + ( oneToMany != null ? oneToMany.hashCode() : 0 );
		result = 31 * result + ( manyToOne != null ? manyToOne.hashCode() : 0 );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if( this == obj ) {
			return true;
		}
		if( !( obj instanceof ManyToManyCompositeKey ) ) {
			return false;
		}

		ManyToManyCompositeKey m = (ManyToManyCompositeKey) obj;
		if ( oneToMany != null ? !oneToMany.equals( m.oneToMany ) : m.oneToMany != null ) {
			return false;
		}
		if ( manyToOne != null ? !manyToOne.equals( m.manyToOne ) : m.manyToOne != null ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "ManyToManyCompositeKey(oneToMany = " +
				oneToMany.toString() +
				", manyToOne = " +
				manyToOne.toString() +
				")";
	}

	public static class ManyToManyId implements Serializable {
		private OneToManyOwned oneToMany;

		private ManyToOneOwned manyToOne;

		ManyToManyId() {
		}

		ManyToManyId(OneToManyOwned oneToMany, ManyToOneOwned manyToOne) {
			this.oneToMany = oneToMany;
			this.manyToOne = manyToOne;
		}

		public OneToManyOwned getOneToMany() {
			return this.oneToMany;
		}

		public ManyToOneOwned getManyToOne() {
			return this.manyToOne;
		}

		@Override
		public int hashCode() {
			int result = 3;
			result = 17 * result + ( oneToMany != null ? oneToMany.hashCode() : 0 );
			result = 17 * result + ( manyToOne != null ? manyToOne.hashCode() : 0 );
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if( this == obj ) {
				return true;
			}
			if( !( obj instanceof  ManyToManyId ) ) {
				return false;
			}

			ManyToManyId m = (ManyToManyId) obj;
			if ( oneToMany != null ? !oneToMany.equals( m.oneToMany ) : m.oneToMany != null ) {
				return false;
			}
			if ( manyToOne != null ? !manyToOne.equals( m.manyToOne ) : m.manyToOne != null ) {
				return false;
			}
			return true;
		}
	}
}
