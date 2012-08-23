/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.collectionalias;

import java.io.Serializable;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;

/**
 * @author Dave Stephan
 */
@Entity
public class TableB implements Serializable
{

	private TableBId id;

	private ATable tablea;
	
	public TableB() {
	}

	/** full constructor */
	public TableB(TableBId id) {
		this.id = id;
	}

	// Property accessors
	@EmbeddedId
	@AttributeOverrides( {
			@AttributeOverride(name = "firstId", column = @Column(name = "idcolumn", nullable = false)),
			@AttributeOverride(name = "secondId", column = @Column(name = "idcolumn_second", nullable = false, length = 50)),
			@AttributeOverride(name = "thirdId", column = @Column(name = "thirdcolumn", nullable = false, length = 20)) })
	public TableBId getId() {
		return this.id;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((tablea == null) ? 0 : tablea.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TableB other = (TableB) obj;
		if (id == null)
		{
			if (other.id != null)
				return false;
		}
		else if (!id.equals(other.id))
			return false;
		if (tablea == null)
		{
			if (other.tablea != null)
				return false;
		}
		else if (!tablea.equals(other.tablea))
			return false;
		return true;
	}

	public void setId(TableBId id) {
		this.id = id;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumns( { @JoinColumn(name = "idcolumn", referencedColumnName = "idcolumn", nullable = false, insertable = false, updatable = false) })
	public ATable getTablea() {
		return tablea;
	}

	public void setTablea(ATable tablea) {
		this.tablea = tablea;
	}

}
