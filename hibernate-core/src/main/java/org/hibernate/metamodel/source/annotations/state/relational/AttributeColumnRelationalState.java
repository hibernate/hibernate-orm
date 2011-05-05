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
package org.hibernate.metamodel.source.annotations.state.relational;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.AnnotationException;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.metamodel.binding.SimpleAttributeBinding;
import org.hibernate.metamodel.relational.Size;
import org.hibernate.metamodel.source.annotations.ColumnValues;
import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.annotations.MappedAttribute;
import org.hibernate.metamodel.source.internal.MetadataImpl;

/**
 * @author Hardy Ferentschik
 */
public class AttributeColumnRelationalState implements SimpleAttributeBinding.ColumnRelationalState {
	private final NamingStrategy namingStrategy;
	private final String columnName;
	private final boolean unique;
	private final boolean nullable;
	private final Size size;
	private final String checkCondition;
	private final String customWriteFragment;
	private final String customReadFragment;
	private final Set<String> indexes;

	// todo - what about these annotations !?
	private String defaultString;
	private String sqlType;
	private String comment;
	private Set<String> uniqueKeys = new HashSet<String>();


	public AttributeColumnRelationalState(MappedAttribute attribute, MetadataImpl meta) {
		ColumnValues columnValues = attribute.getColumnValues();
		namingStrategy = meta.getNamingStrategy();
		columnName = columnValues.getName().isEmpty() ? attribute.getName() : columnValues.getName();
		unique = columnValues.isUnique();
		nullable = columnValues.isNullable();
		size = createSize( columnValues.getLength(), columnValues.getScale(), columnValues.getPrecision() );
		checkCondition = parseCheckAnnotation( attribute );
		indexes = parseIndexAnnotation( attribute );

		String[] readWrite;
		List<AnnotationInstance> columnTransformerAnnotations = getAllColumnTransformerAnnotations( attribute );
		readWrite = createCustomReadWrite( columnTransformerAnnotations );
		customReadFragment = readWrite[0];
		customWriteFragment = readWrite[1];
	}

	@Override
	public NamingStrategy getNamingStrategy() {
		return namingStrategy;
	}

	@Override
	public String getExplicitColumnName() {
		return columnName;
	}

	@Override
	public boolean isUnique() {
		return unique;
	}

	@Override
	public Size getSize() {
		return size;
	}

	@Override
	public boolean isNullable() {
		return nullable;
	}

	@Override
	public String getCheckCondition() {
		return checkCondition;
	}

	@Override
	public String getDefault() {
		return defaultString;
	}

	@Override
	public String getSqlType() {
		return sqlType;
	}

	@Override
	public String getCustomWriteFragment() {
		return customWriteFragment;
	}

	@Override
	public String getCustomReadFragment() {
		return customReadFragment;
	}

	@Override
	public String getComment() {
		return comment;
	}

	@Override
	public Set<String> getUniqueKeys() {
		return uniqueKeys;
	}

	@Override
	public Set<String> getIndexes() {
		return indexes;
	}

	private Size createSize(int length, int scale, int precision) {
		Size size = new Size();
		size.setLength( length );
		size.setScale( scale );
		size.setPrecision( precision );
		return size;
	}

	private List<AnnotationInstance> getAllColumnTransformerAnnotations(MappedAttribute attribute) {
		List<AnnotationInstance> allColumnTransformerAnnotations = new ArrayList<AnnotationInstance>();

		// not quite sure about the usefulness of @ColumnTransformers (HF)
		AnnotationInstance columnTransformersAnnotations = attribute.annotations( HibernateDotNames.COLUMN_TRANSFORMERS );
		if ( columnTransformersAnnotations != null ) {
			AnnotationInstance[] annotationInstances = allColumnTransformerAnnotations.get( 0 ).value().asNestedArray();
			allColumnTransformerAnnotations.addAll( Arrays.asList( annotationInstances ) );
		}

		AnnotationInstance columnTransformerAnnotation = attribute.annotations( HibernateDotNames.COLUMN_TRANSFORMER );
		if ( columnTransformerAnnotation != null ) {
			allColumnTransformerAnnotations.add( columnTransformerAnnotation );
		}
		return allColumnTransformerAnnotations;
	}

	private String[] createCustomReadWrite(List<AnnotationInstance> columnTransformerAnnotations) {
		String[] readWrite = new String[2];

		boolean alreadyProcessedForColumn = false;
		for ( AnnotationInstance annotationInstance : columnTransformerAnnotations ) {
			String forColumn = annotationInstance.value( "forColumn" ) == null ?
					null : annotationInstance.value( "forColumn" ).asString();

			if ( forColumn != null && !forColumn.equals( columnName ) ) {
				continue;
			}

			if ( alreadyProcessedForColumn ) {
				throw new AnnotationException( "Multiple definition of read/write conditions for column " + columnName );
			}

			readWrite[0] = annotationInstance.value( "read" ) == null ?
					null : annotationInstance.value( "read" ).asString();
			readWrite[1] = annotationInstance.value( "write" ) == null ?
					null : annotationInstance.value( "write" ).asString();

			alreadyProcessedForColumn = true;
		}
		return readWrite;
	}

	private String parseCheckAnnotation(MappedAttribute attribute) {
		String checkCondition = null;
		AnnotationInstance checkAnnotation = attribute.annotations( HibernateDotNames.CHECK );
		if ( checkAnnotation != null ) {
			checkCondition = checkAnnotation.value( "constraints" ).toString();
		}
		return checkCondition;
	}

	private Set<String> parseIndexAnnotation(MappedAttribute attribute) {
		Set<String> indexNames = new HashSet<String>();
		AnnotationInstance indexAnnotation = attribute.annotations( HibernateDotNames.INDEX );
		if ( indexAnnotation != null ) {
			String indexName = indexAnnotation.value( "name" ).toString();
			indexNames.add( indexName );
		}
		return indexNames;
	}
}


