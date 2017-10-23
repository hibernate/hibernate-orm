/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
