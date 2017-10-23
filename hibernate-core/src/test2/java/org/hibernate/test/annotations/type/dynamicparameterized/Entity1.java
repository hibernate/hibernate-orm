/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.type.dynamicparameterized;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

/**
 * @author Daniel Gredler
 */
@Entity
@Table(name = "ENTITY1")
@Access(AccessType.FIELD)
public class Entity1 extends AbstractEntity {

	@Column(name = "PROP1")
	String entity1_Prop1;

	@Column(name = "PROP2")
	String entity1_Prop2;

	@Column(name = "PROP3")
	@Type(type = "string", parameters = @Parameter(name = "suffix", value = "foo"))
	String entity1_Prop3;

	@Column(name = "PROP4")
	@Type(type = "string", parameters = @Parameter(name = "suffix", value = "bar"))
	String entity1_Prop4;

	@Column(name = "PROP5")
	@Type(type = "string")
	String entity1_Prop5;

	@Column(name = "PROP6")
	@Type(type = "string")
	String entity1_Prop6;
}
