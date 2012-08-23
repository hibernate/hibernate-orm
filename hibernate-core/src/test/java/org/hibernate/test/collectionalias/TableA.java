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

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Dave Stephan
 */
@Entity
public class TableA
{
	@Id
	private int id;
	
	private String test;
	
	private String test2;

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((test == null) ? 0 : test.hashCode());
		result = prime * result + ((test2 == null) ? 0 : test2.hashCode());
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
		TableA other = (TableA) obj;
		if (id != other.id)
			return false;
		if (test == null)
		{
			if (other.test != null)
				return false;
		}
		else if (!test.equals(other.test))
			return false;
		if (test2 == null)
		{
			if (other.test2 != null)
				return false;
		}
		else if (!test2.equals(other.test2))
			return false;
		return true;
	}

	public String getTest2()
	{
		return test2;
	}

	public void setTest2(String test2)
	{
		this.test2 = test2;
	}

	public String getTest()
	{
		return test;
	}

	public void setTest(String test)
	{
		this.test = test;
	}

	public int getId()
	{
		return id;
	}

	public void setId(int id)
	{
		this.id = id;
	}


}
