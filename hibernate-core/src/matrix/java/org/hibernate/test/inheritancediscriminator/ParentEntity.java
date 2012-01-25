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
package org.hibernate.test.inheritancediscriminator;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;

import static javax.persistence.DiscriminatorType.INTEGER;
import static javax.persistence.GenerationType.IDENTITY;
import static javax.persistence.InheritanceType.SINGLE_TABLE;

/**
 * @author Pawel Stawicki
 */
@Entity
@Inheritance(strategy = SINGLE_TABLE)
@DiscriminatorColumn(name = "CLASS_ID", discriminatorType = INTEGER)
public abstract class ParentEntity  {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "ajdik")
	private Long id;

	public Long getId() {
		return id;
	}
}

