/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.multitenancy.schema;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.id.enhanced.TableGenerator;

/**
 * @author Steve Ebersole
 */
@Entity
public class Invoice {
	@Id
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "number_sequence")
	@GenericGenerator(
			name = "number_sequence",
			strategy = "org.hibernate.id.enhanced.TableGenerator",
			parameters = {
					@Parameter(name = TableGenerator.SEGMENT_VALUE_PARAM, value = "customer"),
					@Parameter(name = TableGenerator.INCREMENT_PARAM, value = "5"),
					@Parameter(name = TableGenerator.OPT_PARAM, value = "pooled")
			}
	)
	private Long id;

	public Long getId() {
		return id;
	}
}
