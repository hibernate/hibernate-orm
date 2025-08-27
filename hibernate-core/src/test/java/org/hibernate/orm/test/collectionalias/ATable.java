/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collectionalias;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

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
