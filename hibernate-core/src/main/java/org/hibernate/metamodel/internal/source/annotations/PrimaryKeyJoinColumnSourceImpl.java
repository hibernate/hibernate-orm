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
package org.hibernate.metamodel.internal.source.annotations;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.source.PrimaryKeyJoinColumnSource;

/**
 * @author Hardy Ferentschik
 */
public class PrimaryKeyJoinColumnSourceImpl implements PrimaryKeyJoinColumnSource {
	private final String columnName;
	private final String referencedColumnName;
	private final String columnDefinition;

	public PrimaryKeyJoinColumnSourceImpl(AnnotationInstance joinColumnAnnotation) {
		this(
				JandexHelper.getValue( joinColumnAnnotation, "name", String.class ),
				JandexHelper.getValue( joinColumnAnnotation, "referencedColumnName", String.class ),
				JandexHelper.getValue( joinColumnAnnotation, "columnDefinition", String.class )
		);
	}

	private PrimaryKeyJoinColumnSourceImpl(
			String columnName,
			String referencedColumnName,
			String columnDefinition) {
		this.columnName = columnName;
		this.referencedColumnName = referencedColumnName;
		this.columnDefinition = columnDefinition;
	}

	@Override
	public String getColumnName() {
		return columnName;
	}

	@Override
	public String getReferencedColumnName() {
		return referencedColumnName;
	}

	@Override
	public String getColumnDefinition() {
		return columnDefinition;
	}
}


