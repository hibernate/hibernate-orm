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
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * @author Dave Stephan
 */
@Entity
public class ATable implements Serializable
{
	private Integer firstId;

	private List<TableB> tablebs = new ArrayList<TableB>();
	
	public ATable()
	{
	}

	/** minimal constructor */
	public ATable(Integer firstId)
	{
		this.firstId = firstId;
	}

	@Id
	@Column(name = "idcolumn", nullable = false)
	public Integer getFirstId()
	{
		return this.firstId;
	}

	public void setFirstId(Integer firstId)
	{
		this.firstId = firstId;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((firstId == null) ? 0 : firstId.hashCode());
		result = prime * result + ((tablebs == null) ? 0 : tablebs.hashCode());
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
		ATable other = (ATable) obj;
		if (firstId == null)
		{
			if (other.firstId != null)
				return false;
		}
		else if (!firstId.equals(other.firstId))
			return false;
		if (tablebs == null)
		{
			if (other.tablebs != null)
				return false;
		}
		else if (!tablebs.equals(other.tablebs))
			return false;
		return true;
	}

	
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "tablea")
	public List<TableB> getTablebs()
	{
		return tablebs;
	}

	public void setTablebs(List<TableB> tablebs)
	{
		this.tablebs = tablebs;
	}
	
	
}
