/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmColumnType;
import org.hibernate.boot.model.TruthValue;
import org.hibernate.boot.model.source.spi.ColumnSource;
import org.hibernate.boot.model.source.spi.DerivedValueSource;
import org.hibernate.boot.model.source.spi.RelationalValueSource;
import org.hibernate.boot.model.source.spi.SizeSource;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;

/**
 * @author Steve Ebersole
 */
public class RelationalValueSourceHelper {

	/**
	 * Internal unifying contract used in creating {@link org.hibernate.mapping.Column}
	 * and {@link org.hibernate.mapping.Formula} instances.  It adapts the variances across
	 * the different mappings which contribute column/formula information.  For example,
	 * consider the {@code <property/>} mapping which might have:<ul>
	 *     <li>a {@code column} XML attribute</li>
	 *     <li>a {@code formula} XML attribute</li>
	 *     <li>one or more nested {@code <column/>} XML elements</li>
	 *     <li>a nested {@code <formula/>} XML element</li>
	 * </ul>
	 * as opposed to a {@code <timestamp/>} mapping, which can only have:<ul>
	 *     <li>a {@code column} XML attribute</li>
	 * </ul>
	 */
	public interface ColumnsAndFormulasSource  {
		/**
		 * What kind of XML element does this information come from?
		 *
		 * @return The source XML element type
		 */
		XmlElementMetadata getSourceType();

		/**
		 * The name of the source.  May be {@code null} if none was specified.  Will be treated
		 * as {@code null} if the nature says it cannot be named.
		 *
		 * @return The name of the source.
		 *
		 * @see XmlElementMetadata#canBeNamed()
		 */
		String getSourceName();

		/**
		 * Access to any formula defined via XML attribute.
		 *
		 * @return formula, if one, as defined via XML attribute.
		 */
		String getFormulaAttribute();

		/**
		 * Access to any nested {@code <column/>} or {@code <formula />} XML elements.
		 *
		 * @return columns or formulas defined via nested XML elements.
		 */
		List getColumnOrFormulaElements();

		/**
		 * Access to any column defined via XML attribute.
		 *
		 * @return column, if one, as defined via XML attribute.
		 */
		String getColumnAttribute();
		SizeSource getSizeSource();

		Boolean isNullable();
		boolean isUnique();

		Set<String> getIndexConstraintNames();

		Set<String> getUniqueKeyConstraintNames();
	}

	public abstract static class AbstractColumnsAndFormulasSource implements ColumnsAndFormulasSource {
		@Override
		public String getFormulaAttribute() {
			return null;
		}

		@Override
		public String getColumnAttribute() {
			return null;
		}

		@Override
		public List getColumnOrFormulaElements() {
			return Collections.emptyList();
		}

		@Override
		public SizeSource getSizeSource() {
			return null;
		}

		@Override
		public Boolean isNullable() {
			return null;
		}

		@Override
		public Set<String> getIndexConstraintNames() {
			return Collections.emptySet();
		}

		@Override
		public boolean isUnique() {
			return false;
		}

		@Override
		public Set<String> getUniqueKeyConstraintNames() {
			return Collections.emptySet();
		}
	}

	/**
	 * Given a {@link ColumnsAndFormulasSource}, build a single {@link RelationalValueSource}.
	 * More than one {@link RelationalValueSource} will result in an exception.
	 *
	 * @param mappingDocument the mapping document
	 * @param containingTableName The logical name of the table containing the relational values
	 * @param columnsAndFormulasSource the adapter describing the value sources.
	 *
	 * @return The single RelationalValueSource.
	 */
	public static RelationalValueSource buildValueSource(
			MappingDocument mappingDocument,
			String containingTableName,
			RelationalValueSourceHelper.ColumnsAndFormulasSource columnsAndFormulasSource) {
		final List<RelationalValueSource> sources = buildValueSources(
				mappingDocument,
				containingTableName,
				columnsAndFormulasSource
		);

		if ( sources.size() > 1 ) {
			final String errorMessage;
			if ( columnsAndFormulasSource.getSourceType().canBeNamed()
					&& StringHelper.isNotEmpty( columnsAndFormulasSource.getSourceName() ) ) {
				errorMessage = String.format(
						Locale.ENGLISH,
						"Expecting just a single formula/column in context of <%s name=\"%s\"/>",
						columnsAndFormulasSource.getSourceType().getElementName(),
						columnsAndFormulasSource.getSourceName()
				);
			}
			else {
				errorMessage = String.format(
						Locale.ENGLISH,
						"Expecting just a single formula/column in context of <%s/>",
						columnsAndFormulasSource.getSourceType().getElementName()
				);
			}
			throw new MappingException( errorMessage, mappingDocument.getOrigin() );
		}

		return sources.get( 0 );
	}

	/**
	 * Given a {@link ColumnsAndFormulasSource}, build a single {@link RelationalValueSource}
	 * which is required to be a column.  More than one {@link RelationalValueSource} will result
	 * in an exception.  A formula, rather than a column, will result in an exception.
	 *
	 * @param mappingDocument the mapping document
	 * @param containingTableName The logical name of the table containing the relational values
	 * @param columnsAndFormulasSource the adapter describing the value sources.
	 *
	 * @return The single ColumnSource.
	 */
	public static ColumnSource buildColumnSource(
			MappingDocument mappingDocument,
			String containingTableName,
			RelationalValueSourceHelper.ColumnsAndFormulasSource columnsAndFormulasSource) {
		final List<RelationalValueSource> sources = buildValueSources(
				mappingDocument,
				containingTableName,
				columnsAndFormulasSource
		);

		if ( sources.size() > 1 ) {
			final String errorMessage;
			if ( columnsAndFormulasSource.getSourceType().canBeNamed()
					&& StringHelper.isNotEmpty( columnsAndFormulasSource.getSourceName() ) ) {
				errorMessage = String.format(
						Locale.ENGLISH,
						"Expecting just a single formula/column in context of <%s name=\"%s\"/>",
						columnsAndFormulasSource.getSourceType().getElementName(),
						columnsAndFormulasSource.getSourceName()
				);
			}
			else {
				errorMessage = String.format(
						Locale.ENGLISH,
						"Expecting just a single formula/column in context of <%s/>",
						columnsAndFormulasSource.getSourceType().getElementName()
				);
			}
			throw new MappingException( errorMessage, mappingDocument.getOrigin() );
		}

		final RelationalValueSource result = sources.get( 0 );
		if ( !ColumnSource.class.isInstance( result ) ) {
			final String errorMessage;
			if ( columnsAndFormulasSource.getSourceType().canBeNamed()
					&& StringHelper.isNotEmpty( columnsAndFormulasSource.getSourceName() ) ) {
				errorMessage = String.format(
						Locale.ENGLISH,
						"Expecting single column in context of <%s name=\"%s\"/>, but found formula [%s]",
						columnsAndFormulasSource.getSourceType().getElementName(),
						columnsAndFormulasSource.getSourceName(),
						( (DerivedValueSource) result ).getExpression()
				);
			}
			else {
				errorMessage = String.format(
						Locale.ENGLISH,
						"Expecting single column in context of <%s/>, but found formula [%s]",
						columnsAndFormulasSource.getSourceType().getElementName(),
						( (DerivedValueSource) result ).getExpression()
				);
			}
			throw new MappingException( errorMessage, mappingDocument.getOrigin() );
		}

		return (ColumnSource) result;
	}

	/**
	 * Given a {@link ColumnsAndFormulasSource}, build the corresponding list of
	 * {@link ColumnSource}.  Any formula, rather than a column, will result in an exception.
	 *
	 * @param mappingDocument the mapping document
	 * @param containingTableName The logical name of the table containing the relational values
	 * @param columnsAndFormulasSource the adapter describing the value sources.
	 *
	 * @return The corresponding list.
	 */
	public static List<ColumnSource> buildColumnSources(
			MappingDocument mappingDocument,
			String containingTableName,
			RelationalValueSourceHelper.ColumnsAndFormulasSource columnsAndFormulasSource) {
		final List<RelationalValueSource> sources = buildValueSources(
				mappingDocument,
				containingTableName,
				columnsAndFormulasSource
		);

		final List<ColumnSource> columnSources = CollectionHelper.arrayList( sources.size() );
		for ( RelationalValueSource source : sources ) {
			if ( !ColumnSource.class.isInstance( source ) ) {
				final String errorMessage;
				if ( columnsAndFormulasSource.getSourceType().canBeNamed()
						&& StringHelper.isNotEmpty( columnsAndFormulasSource.getSourceName() ) ) {
					errorMessage = String.format(
							Locale.ENGLISH,
							"Expecting only columns in context of <%s name=\"%s\"/>, but found formula [%s]",
							columnsAndFormulasSource.getSourceType().getElementName(),
							columnsAndFormulasSource.getSourceName(),
							( (DerivedValueSource) source ).getExpression()
					);
				}
				else {
					errorMessage = String.format(
							Locale.ENGLISH,
							"Expecting only columns in context of <%s/>, but found formula [%s]",
							columnsAndFormulasSource.getSourceType().getElementName(),
							( (DerivedValueSource) source ).getExpression()
					);
				}
				throw new MappingException( errorMessage, mappingDocument.getOrigin() );
			}
			columnSources.add( (ColumnSource) source );
		}
		return columnSources;
	}

	/**
	 * Given a {@link ColumnsAndFormulasSource}, build the corresponding list of
	 * {@link RelationalValueSource}
	 *
	 * @param mappingDocument the mapping document
	 * @param containingTableName The logical name of the table containing the relational values
	 * @param columnsAndFormulasSource the adapter describing the value sources.
	 *
	 * @return The corresponding list.
	 */
	public static List<RelationalValueSource> buildValueSources(
			MappingDocument mappingDocument,
			String containingTableName,
			RelationalValueSourceHelper.ColumnsAndFormulasSource columnsAndFormulasSource) {
		List<RelationalValueSource> result = new ArrayList<RelationalValueSource>();

		if ( StringHelper.isNotEmpty( columnsAndFormulasSource.getFormulaAttribute() ) ) {
			// we have an explicit formula attribute (i.e., <SOMETHING formula="abc"/>)
			validateUseOfFormulaAttribute( mappingDocument, columnsAndFormulasSource );

			result.add(
					new FormulaImpl(
							mappingDocument,
							containingTableName,
							columnsAndFormulasSource.getFormulaAttribute()
					)
			);
		}
		else if ( CollectionHelper.isNotEmpty( columnsAndFormulasSource.getColumnOrFormulaElements() ) ) {
			validateUseOfColumnOrFormulaNestedElements( mappingDocument, columnsAndFormulasSource );

			for ( Object selectable : columnsAndFormulasSource.getColumnOrFormulaElements() ) {
				if ( selectable instanceof JaxbHbmColumnType ) {
					final JaxbHbmColumnType columnElement = (JaxbHbmColumnType) selectable;
					result.add(
							new ColumnSourceImpl(
									mappingDocument,
									containingTableName,
									columnElement,
									columnsAndFormulasSource.getIndexConstraintNames(),
									columnsAndFormulasSource.getUniqueKeyConstraintNames()
							)
					);
				}
				else if ( selectable instanceof String ) {
					result.add(
							new FormulaImpl( mappingDocument, containingTableName, (String) selectable )
					);
				}
				else {
					throw new MappingException(
							"Unexpected column/formula JAXB type : " + selectable.getClass().getName(),
							mappingDocument.getOrigin()
					);
				}
			}
		}
		else {
			// we have either an explicitly named column via the column attribute, or an implicit
			// column reference.  Aside from applying an implicit naming strategy (or not), these 2
			// case are handled the exact same way

			result.add(
					new ColumnAttributeSourceImpl(
							mappingDocument,
							containingTableName,
							columnsAndFormulasSource.getColumnAttribute(),
							columnsAndFormulasSource.getSizeSource(),
							interpretNullabilityToTruthValue( columnsAndFormulasSource.isNullable() ),
							columnsAndFormulasSource.isUnique() ? TruthValue.TRUE : TruthValue.FALSE,
							columnsAndFormulasSource.getIndexConstraintNames(),
							columnsAndFormulasSource.getUniqueKeyConstraintNames()
					)
			);
		}

		return result;
	}

	private static TruthValue interpretNullabilityToTruthValue(Boolean nullable) {
		if ( nullable == null ) {
			return TruthValue.UNKNOWN;
		}
		else {
			return nullable ? TruthValue.TRUE : TruthValue.FALSE;
		}
	}

	private static void validateUseOfFormulaAttribute(
			MappingDocument sourceDocument,
			RelationalValueSourceHelper.ColumnsAndFormulasSource columnsAndFormulasSource) {
		//		1) make sure there is no column attribute
		if ( StringHelper.isNotEmpty( columnsAndFormulasSource.getColumnAttribute() ) ) {
			final String errorMessage;
			if ( columnsAndFormulasSource.getSourceType().canBeNamed()
					&& StringHelper.isNotEmpty( columnsAndFormulasSource.getSourceName() ) ) {
				errorMessage = String.format(
						Locale.ENGLISH,
						"column attribute and formula attribute may not be specified together near <%s name=\"%s\" column=\"%s\" formula=\"%s\" />",
						columnsAndFormulasSource.getSourceType().getElementName(),
						columnsAndFormulasSource.getSourceName(),
						columnsAndFormulasSource.getColumnAttribute(),
						columnsAndFormulasSource.getFormulaAttribute()
				);
			}
			else {
				errorMessage = String.format(
						Locale.ENGLISH,
						"column attribute and formula attribute may not be specified together near <%s column=\"%s\" formula=\"%s\" />",
						columnsAndFormulasSource.getSourceType().getElementName(),
						columnsAndFormulasSource.getColumnAttribute(),
						columnsAndFormulasSource.getFormulaAttribute()
				);
			}
			throw new MappingException( errorMessage, sourceDocument.getOrigin() );
		}
		//		2) and no column/formula nested elements
		if ( CollectionHelper.isNotEmpty( columnsAndFormulasSource.getColumnOrFormulaElements() ) ) {
			final String errorMessage;
			if ( columnsAndFormulasSource.getSourceType().canBeNamed()
					&& StringHelper.isNotEmpty( columnsAndFormulasSource.getSourceName() ) ) {
				errorMessage = String.format(
						Locale.ENGLISH,
						"formula attribute may not be specified along with <column/> or <formula/> subelement(s) near <%s name=\"%s\" formula=\"%s\" />",
						columnsAndFormulasSource.getSourceType().getElementName(),
						columnsAndFormulasSource.getSourceName(),
						columnsAndFormulasSource.getFormulaAttribute()
				);
			}
			else {
				errorMessage = String.format(
						Locale.ENGLISH,
						"formula attribute may not be specified along with <column/> or <formula/> subelement(s) near <%s formula=\"%s\" />",
						columnsAndFormulasSource.getSourceType().getElementName(),
						columnsAndFormulasSource.getFormulaAttribute()
				);
			}
			throw new MappingException( errorMessage, sourceDocument.getOrigin() );
		}
	}

	private static void validateUseOfColumnOrFormulaNestedElements(
			MappingDocument sourceDocument,
			RelationalValueSourceHelper.ColumnsAndFormulasSource columnsAndFormulasSource) {
		if ( StringHelper.isNotEmpty( columnsAndFormulasSource.getColumnAttribute() ) ) {
			final String errorMessage;
			if ( columnsAndFormulasSource.getSourceType().canBeNamed()
					&& StringHelper.isNotEmpty( columnsAndFormulasSource.getSourceName() ) ) {
				errorMessage = String.format(
						Locale.ENGLISH,
						"column attribute may not be specified along with <column/> or <formula/> subelement(s) near <%s name=\"%s\" column=\"%s\" />",
						columnsAndFormulasSource.getSourceType().getElementName(),
						columnsAndFormulasSource.getSourceName(),
						columnsAndFormulasSource.getColumnAttribute()
				);
			}
			else {
				errorMessage = String.format(
						Locale.ENGLISH,
						"column attribute may not be specified along with <column/> or <formula/> subelement(s) near <%s column=\"%s\" />",
						columnsAndFormulasSource.getSourceType().getElementName(),
						columnsAndFormulasSource.getColumnAttribute()
				);
			}
			throw new MappingException( errorMessage, sourceDocument.getOrigin() );
		}
	}

	private static void validateCustomWriteFragment(
			MappingDocument sourceDocument,
			RelationalValueSourceHelper.ColumnsAndFormulasSource columnsAndFormulasSource,
			JaxbHbmColumnType columnMapping,
			String customWrite) {
		if ( customWrite != null && !customWrite.matches("[^?]*\\?[^?]*") ) {
			final String errorMessage;
			if ( columnsAndFormulasSource.getSourceType().canBeNamed()
					&& StringHelper.isNotEmpty( columnsAndFormulasSource.getSourceName() ) ) {
				errorMessage = String.format(
						Locale.ENGLISH,
						"write expression must contain exactly one value placeholder ('?') character near <column name=\"%s\" ... write=\"%s\" /> for <%s name=\"%s\" />",
						columnMapping.getName(),
						customWrite,
						columnsAndFormulasSource.getSourceType().getElementName(),
						columnsAndFormulasSource.getSourceName()
				);
			}
			else {
				errorMessage = String.format(
						Locale.ENGLISH,
						"write expression must contain exactly one value placeholder ('?') character near <column name=\"%s\" ... write=\"%s\" /> for <%s />",
						columnMapping.getName(),
						customWrite,
						columnsAndFormulasSource.getSourceType().getElementName()
				);
			}
			throw new MappingException( errorMessage, sourceDocument.getOrigin() );
		}
	}

}
