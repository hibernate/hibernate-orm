/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.cascade;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Table;

import org.hibernate.annotations.ForeignKey;

@Entity
@Table(name = "CODED_PAIR_SET_HOLDER")
class CodedPairSetHolder implements Serializable {

	private static final long serialVersionUID = 3757670856634990003L;

	@Id
	@GeneratedValue
	@Column(name = "ID")
	private Long id;

	@Column(name = "CODE", nullable = false, unique = true, updatable = false, length = 256)
	private String code;

	@ElementCollection
	@JoinTable(name = "CODED_PAIR_HOLDER_PAIR_SET", joinColumns = @JoinColumn(name = "CODED_PAIR_HOLDER_ID"))
	@ForeignKey(name = "FK_PAIR_SET")
	private final Set<PersonPair> pairs = new HashSet<PersonPair>(0);

	CodedPairSetHolder() {
		super();
	}

	CodedPairSetHolder(final String pCode, final Set<PersonPair> pPersonPairs) {
		super();
		this.code = pCode;
		this.pairs.addAll(pPersonPairs);
	}

	Long getId() {
		return this.id;
	}

	String getCode() {
		return this.code;
	}

	Set<PersonPair> getPairs() {
		return Collections.unmodifiableSet(this.pairs);
	}

	@Override
	public int hashCode() {
		final int prime = 101;
		int result = 1;
		result = prime * result + ((getCode() == null) ? 0 : getCode().hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object pObject) {
		if (this == pObject) {
			return true;
		}
		if (pObject == null) {
			return false;
		}
		if (!(pObject instanceof CodedPairSetHolder )) {
			return false;
		}
		final CodedPairSetHolder other = (CodedPairSetHolder) pObject;
		if (getCode() == null) {
			if (other.getCode() != null) {
				return false;
			}
		} else if (!getCode().equals(other.getCode())) {
			return false;
		}
		return true;
	}

}
