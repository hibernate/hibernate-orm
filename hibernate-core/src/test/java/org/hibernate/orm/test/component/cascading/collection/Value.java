/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component.cascading.collection;


/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class Value {
	private Integer id;
	private Definition definition;
	private LocalizedStrings localizedStrings = new LocalizedStrings();

	protected Value() {
	}

	public Value(Integer id, Definition definition) {
		this.id = id;
		this.definition = definition;
		definition.getValues().add( this );
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Definition getDefinition() {
		return definition;
	}

	public void setDefinition(Definition definition) {
		this.definition = definition;
	}

	public LocalizedStrings getLocalizedStrings() {
		return localizedStrings;
	}

	public void setLocalizedStrings(LocalizedStrings localizedStrings) {
		this.localizedStrings = localizedStrings;
	}
}
