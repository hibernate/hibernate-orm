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
package org.hibernate.metamodel.source.annotations.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.junit.Test;

import org.hibernate.annotations.RowId;
import org.hibernate.metamodel.binding.EntityBinding;

import static junit.framework.Assert.assertEquals;

/**
 * Tests for {@code o.h.a.RowId}.
 *
 * @author Hardy Ferentschik
 */
public class RowIdBindingTests extends BaseAnnotationBindingTestCase {
	@Test
	public void testNoRowId() {
		buildMetadataSources( NoRowIdEntity.class );
		EntityBinding binding = getEntityBinding( NoRowIdEntity.class );
		assertEquals( "Wrong row id", null, binding.getRowId() );
	}

	@Test
	public void testRowId() {
		buildMetadataSources( RowIdEntity.class );
		EntityBinding binding = getEntityBinding( RowIdEntity.class );
		assertEquals( "Wrong row id", "rowid", binding.getRowId() );
	}

	@Entity
	class NoRowIdEntity {
		@Id
		private int id;
	}

	@Entity
	@RowId("rowid")
	class RowIdEntity {
		@Id
		private int id;
	}
}


