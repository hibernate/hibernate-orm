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
package org.hibernate.metamodel.internal.source.annotations.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.junit.Test;

import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.metamodel.spi.binding.CustomSQL;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.testing.junit4.BaseAnnotationBindingTestCase;
import org.hibernate.testing.junit4.Resources;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

/**
 * Tests for {@code o.h.a.SQLInsert}, {@code o.h.a.SQLUpdate}, {@code o.h.a.Delete} and {@code o.h.a.SQLDeleteAll}.
 *
 * @author Hardy Ferentschik
 */
public class CustomSQLBindingTest extends BaseAnnotationBindingTestCase {
	@Test
	@Resources(annotatedClasses = NoCustomSQLEntity.class)
	public void testNoCustomSqlAnnotations() {
		EntityBinding binding = getEntityBinding( NoCustomSQLEntity.class );
		assertNull( binding.getCustomDelete() );
		assertNull( binding.getCustomInsert() );
		assertNull( binding.getCustomUpdate() );
	}

	@Test
	@Resources(annotatedClasses = CustomSQLEntity.class)
	public void testCustomSqlAnnotations() {
		EntityBinding binding = getEntityBinding( CustomSQLEntity.class );

		CustomSQL customSql = binding.getCustomInsert();
		assertCustomSql( customSql, "INSERT INTO FOO", true, ExecuteUpdateResultCheckStyle.NONE );

		customSql = binding.getCustomDelete();
		assertCustomSql( customSql, "DELETE FROM FOO", false, ExecuteUpdateResultCheckStyle.COUNT );

		customSql = binding.getCustomUpdate();
		assertCustomSql( customSql, "UPDATE FOO", false, ExecuteUpdateResultCheckStyle.PARAM );
	}

// not so sure about the validity of this one
//	@Test
//	public void testDeleteAllWins() {
//		buildMetadataSources( CustomDeleteAllEntity.class );
//		EntityBinding binding = getEntityBinding( CustomDeleteAllEntity.class );
//		assertEquals( "Wrong sql", "DELETE ALL", binding.getCustomDelete().getSql() );
//	}

	private void assertCustomSql(CustomSQL customSql, String sql, boolean isCallable, ExecuteUpdateResultCheckStyle style) {
		assertNotNull( customSql );
		assertEquals( "Wrong sql", sql, customSql.getSql() );
		assertEquals( isCallable, customSql.isCallable() );
		assertEquals( style, customSql.getCheckStyle() );
	}

	@Entity
	class NoCustomSQLEntity {
		@Id
		private int id;
	}

	@Entity
	@SQLInsert(sql = "INSERT INTO FOO", callable = true)
	@SQLDelete(sql = "DELETE FROM FOO", check = ResultCheckStyle.COUNT)
	@SQLUpdate(sql = "UPDATE FOO", check = ResultCheckStyle.PARAM)
	class CustomSQLEntity {
		@Id
		private int id;
	}

	@Entity
	@SQLDelete(sql = "DELETE")
	@SQLDeleteAll(sql = "DELETE ALL")
	class CustomDeleteAllEntity {
		@Id
		private int id;
	}
}


