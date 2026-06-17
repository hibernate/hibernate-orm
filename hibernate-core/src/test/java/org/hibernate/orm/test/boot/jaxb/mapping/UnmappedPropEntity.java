/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping;

public class UnmappedPropEntity {
	private Long id;
	private String name;
	private UnmappedPropTarget unmappedRef;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public UnmappedPropTarget getUnmappedRef() {
		return unmappedRef;
	}

	public void setUnmappedRef(UnmappedPropTarget unmappedRef) {
		this.unmappedRef = unmappedRef;
	}

	public void setCompositeName(String part1, String part2){
		this.name = part1 + part2;
	}

	public String getCompositeName(){
		return name;
	}

	public void setAnotherCompositeName(String part1, String part2){
		this.name = part1 + part2;
	}

	public String getAnotherCompositeName(){
		return name;
	}
}
