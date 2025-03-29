/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cascade;

import java.util.HashSet;
import java.util.Set;

public class A
{
	// Constants -----------------------------------------------------------------------------------

	// Static --------------------------------------------------------------------------------------

	// Attributes ----------------------------------------------------------------------------------

	private long id;

	private String data;

	// A 1 - * H
	private Set hs;

	// A 1 - 1 G
	private G g;


	// Constructors --------------------------------------------------------------------------------

	public A()
	{
		hs = new HashSet();
	}

	public A(String data)
	{
		this();
		this.data = data;
	}

	// Public --------------------------------------------------------------------------------------

	public long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public void setData(String data)
	{
		this.data = data;
	}

	public String getData()
	{
		return data;
	}

	public void setHs(Set hs)
	{
		this.hs = hs;
	}

	public Set getHs()
	{
		return hs;
	}

	public void setG(G g)
	{
		this.g = g;
	}

	public G getG()
	{
		return g;
	}

	public void addH(H h)
	{
		hs.add(h);
		h.setA(this);
	}

	public String toString()
	{
		return "A[" + id + ", " + data + "]";
	}

	// Package protected ---------------------------------------------------------------------------

	// Protected -----------------------------------------------------------------------------------

	// Private -------------------------------------------------------------------------------------

	// Inner classes -------------------------------------------------------------------------------
}
