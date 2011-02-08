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
package org.hibernate.cfg.annotations;
import java.lang.annotation.Annotation;
import javax.persistence.Column;
import javax.persistence.MapKeyColumn;

/**
 * @author Emmanuel Bernard
 */
@SuppressWarnings({ "ClassExplicitlyAnnotation" })
public class MapKeyColumnDelegator implements Column {
	private final MapKeyColumn column;

	public MapKeyColumnDelegator(MapKeyColumn column) {
		this.column = column;
	}

	public String name() {
		return column.name();
	}

	public boolean unique() {
		return column.unique();
	}

	public boolean nullable() {
		return column.nullable();
	}

	public boolean insertable() {
		return column.insertable();
	}

	public boolean updatable() {
		return column.updatable();
	}

	public String columnDefinition() {
		return column.columnDefinition();
	}

	public String table() {
		return column.table();
	}

	public int length() {
		return column.length();
	}

	public int precision() {
		return column.precision();
	}

	public int scale() {
		return column.scale();
	}

	public Class<? extends Annotation> annotationType() {
		return Column.class;
	}
}
