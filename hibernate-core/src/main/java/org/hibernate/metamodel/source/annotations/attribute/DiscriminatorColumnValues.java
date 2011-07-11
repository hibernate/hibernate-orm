/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.annotations.attribute;

import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.JandexHelper;

/**
 * Container for the properties of a discriminator column.
 *
 * @author Hardy Ferentschik
 */
public class DiscriminatorColumnValues extends ColumnValues {
	public static final String DEFAULT_DISCRIMINATOR_COLUMN_NAME = "DTYPE";
	private static final int DEFAULT_DISCRIMINATOR_LENGTH = 31;

	private boolean isForced = true;
	private boolean isIncludedInSql = true;
	private String discriminatorValue = null;

	public DiscriminatorColumnValues(Map<DotName, List<AnnotationInstance>> annotations) {
		super();

		AnnotationInstance discriminatorOptionsAnnotation = JandexHelper.getSingleAnnotation(
				annotations, JPADotNames.DISCRIMINATOR_COLUMN
		);

		if ( discriminatorOptionsAnnotation != null ) {
			setName( discriminatorOptionsAnnotation.value( "name" ).asString() );
			setLength( discriminatorOptionsAnnotation.value( "length" ).asInt() );
			if ( discriminatorOptionsAnnotation.value( "columnDefinition" ) != null ) {
				setColumnDefinition( discriminatorOptionsAnnotation.value( "columnDefinition" ).asString() );
			}
		}
		else {
			setName( DEFAULT_DISCRIMINATOR_COLUMN_NAME );
			setLength( DEFAULT_DISCRIMINATOR_LENGTH );
		}

		setNullable( false );
		setDiscriminatorValue( annotations );
		setDiscriminatorOptions( annotations );
		setDiscriminatorFormula( annotations );
	}

	private void setDiscriminatorValue(Map<DotName, List<AnnotationInstance>> annotations) {
		AnnotationInstance discriminatorValueAnnotation = JandexHelper.getSingleAnnotation(
				annotations, JPADotNames.DISCRIMINATOR_VALUE
		);
		if ( discriminatorValueAnnotation != null ) {
			discriminatorValue = discriminatorValueAnnotation.value().asString();
		}
	}

	private void setDiscriminatorFormula(Map<DotName, List<AnnotationInstance>> annotations) {
		AnnotationInstance discriminatorFormulaAnnotation = JandexHelper.getSingleAnnotation(
				annotations, HibernateDotNames.DISCRIMINATOR_FORMULA
		);
		if ( discriminatorFormulaAnnotation != null ) {
			// todo
		}
	}

	public boolean isForced() {
		return isForced;
	}

	public boolean isIncludedInSql() {
		return isIncludedInSql;
	}

	public String getDiscriminatorValue() {
		return discriminatorValue;
	}

	private void setDiscriminatorOptions(Map<DotName, List<AnnotationInstance>> annotations) {
		AnnotationInstance discriminatorOptionsAnnotation = JandexHelper.getSingleAnnotation(
				annotations, HibernateDotNames.DISCRIMINATOR_OPTIONS
		);
		if ( discriminatorOptionsAnnotation != null ) {
			isForced = discriminatorOptionsAnnotation.value( "force" ).asBoolean();
			isIncludedInSql = discriminatorOptionsAnnotation.value( "insert" ).asBoolean();
		}
	}
}


