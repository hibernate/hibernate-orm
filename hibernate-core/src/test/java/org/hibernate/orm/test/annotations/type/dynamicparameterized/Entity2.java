/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.type.dynamicparameterized;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.Parameter;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * @author Daniel Gredler
 */
@Entity
@Table(name = "ENTITY2")
@Access(AccessType.FIELD)
public class Entity2 extends AbstractEntity {

	@Column(name = "PROP1")
	@Type( MyStringType.class )
	String entity2_Prop1;

	@Column(name = "PROP2")
	@Type( MyStringType.class )
	String entity2_Prop2;

	@Column(name = "PROP3")
	@Type( MyStringType.class )
	String entity2_Prop3;

	@Column(name = "PROP4")
	@Type( MyStringType.class )
	String entity2_Prop4;

	@Column(name = "PROP5")
	@Type( value = MyStringType.class, parameters = @Parameter(name = "suffix", value = "blah"))
	String entity2_Prop5;

	@Column(name = "PROP6")
	@Type( value = MyStringType.class, parameters = @Parameter(name = "suffix", value = "yeah"))
	String entity2_Prop6;
}
