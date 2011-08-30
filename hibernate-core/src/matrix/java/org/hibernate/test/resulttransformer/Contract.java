/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-
 * party contributors as indicated by the @author tags or express 
 * copyright attribution statements applied by the authors.  
 * All third-party contributions are distributed under license by 
 * Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to 
 * use, modify, copy, or redistribute it subject to the terms and 
 * conditions of the GNU Lesser General Public License, as published 
 * by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU 
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this distribution; if not, write to:
 * 
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.test.resulttransformer;


/**
 * @author Sharath Reddy
 *
 */
public class Contract {

	private Long id;
	private String name;
	private PartnerA a;
	private PartnerB b;
	private Long custom1;

	public Long getId()
	{
		return id;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public PartnerA getA()
	{
		return a;
	}

	public void setA(PartnerA a)
	{
		this.a = a;
	}

	public PartnerB getB()
	{
		return b;
	}

	public void setB(PartnerB b)
	{
		this.b = b;
	}

	public Long getCustom1()
	{
		return custom1;
	}

	public void setCustom1(Long custom1)
	{
		this.custom1 = custom1;
	}
}
