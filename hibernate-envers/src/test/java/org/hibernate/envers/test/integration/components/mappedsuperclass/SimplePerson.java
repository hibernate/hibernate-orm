/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers.test.integration.components.mappedsuperclass;


import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.envers.Audited;

/**
 * @author Jakob Braeuchi.
 */
@Entity
@Table(name = "TEST_SIMPLE_PERSON")
@Access(AccessType.FIELD)
@Audited
public class SimplePerson {

	@Id
	@GeneratedValue
	private long id;

	@Version
	private long version;

	@Column(name = "NAME", length = 100)
	private String name;

	@Embedded
	@AttributeOverrides({ @AttributeOverride(name = "code", column = @Column(name = "THE_TEST")) })
	private TestCode testCode = TestCode.TEST;

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "code", column = @Column(name = "THE_CODE")),
			@AttributeOverride(name = "codeArt", column = @Column(name = "THE_CODEART"))
	})
	private Code genericCode;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getVersion() {
		return version;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public TestCode getTestCode() {
		return testCode;
	}

	public void setTestCode(TestCode testCode) {
		this.testCode = testCode;
	}

	public Code getGenericCode() {
		return genericCode;
	}

	public void setGenericCode(Code genericCode) {
		this.genericCode = genericCode;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) ( id ^ ( id >>> 32 ) );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		SimplePerson other = (SimplePerson) obj;
		if ( id != other.id ) {
			return false;
		}
		return true;
	}
}
