/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.resulttransformer;


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
