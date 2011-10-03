/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.naturalid;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * @author Guenther Demetz
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
public class D {
	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	public long oid;

	@Version
	private int version;

	@javax.persistence.ManyToOne(fetch = FetchType.LAZY)
	private A a = null;

	@javax.persistence.OneToOne(mappedBy = "singleD")
	private A singleA = null;

	public A getA() {
		return a;
	}

	public void setA(A a) {
		this.a = a;
	}

	public A getSingleA() {
		return singleA;
	}

	public void setSingleA(A singleA) {
		this.singleA = singleA;
	}
}
