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
@Table(name = "ENTITY1")
@Access(AccessType.FIELD)
public class Entity1 extends AbstractEntity {

	@Column(name = "PROP1")
	@Type( MyStringType.class )
	String entity1_Prop1;

	@Column(name = "PROP2")
	@Type( MyStringType.class )
	String entity1_Prop2;

	@Column(name = "PROP3")
	@Type( value = MyStringType.class, parameters = @Parameter(name = "suffix", value = "foo"))
	String entity1_Prop3;

	@Column(name = "PROP4")
	@Type( value = MyStringType.class, parameters = @Parameter(name = "suffix", value = "bar"))
	String entity1_Prop4;

	@Column(name = "PROP5")
	@Type( MyStringType.class )
	String entity1_Prop5;

	@Column(name = "PROP6")
	@Type( MyStringType.class )
	String entity1_Prop6;
}
