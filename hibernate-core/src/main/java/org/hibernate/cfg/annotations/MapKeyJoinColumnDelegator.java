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
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyJoinColumn;

/**
 * @author Emmanuel Bernard
 */
@SuppressWarnings({ "ClassExplicitlyAnnotation" })
public class MapKeyJoinColumnDelegator implements JoinColumn {
	private final MapKeyJoinColumn column;

	public MapKeyJoinColumnDelegator(MapKeyJoinColumn column) {
		this.column = column;
	}

	public String name() {
		return column.name();
	}

	public String referencedColumnName() {
		return column.referencedColumnName();
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

	public Class<? extends Annotation> annotationType() {
		return Column.class;
	}
}
