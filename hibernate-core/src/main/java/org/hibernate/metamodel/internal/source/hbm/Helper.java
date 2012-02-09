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
package org.hibernate.metamodel.internal.source.hbm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.internal.jaxb.mapping.hbm.CustomSqlElement;
import org.hibernate.internal.jaxb.mapping.hbm.EntityElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbColumnElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbJoinedSubclassElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbMetaElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbParamElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbSubclassElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbUnionSubclassElement;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.spi.binding.CustomSQL;
import org.hibernate.metamodel.spi.binding.InheritanceType;
import org.hibernate.metamodel.spi.binding.MetaAttribute;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.Schema;
import org.hibernate.metamodel.spi.source.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.spi.source.LocalBindingContext;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.metamodel.spi.source.MetaAttributeContext;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;

/**
 * A helper for dealing with
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class Helper {
	public static final ExplicitHibernateTypeSource TO_ONE_ATTRIBUTE_TYPE_SOURCE = new ExplicitHibernateTypeSource() {
		@Override
		public String getName() {
			return null;
		}

		@Override
		public Map<String, String> getParameters() {
			return null;
		}
	};

	public static InheritanceType interpretInheritanceType(EntityElement entityElement) {
		if ( JaxbSubclassElement.class.isInstance( entityElement ) ) {
			return InheritanceType.SINGLE_TABLE;
		}
		else if ( JaxbJoinedSubclassElement.class.isInstance( entityElement ) ) {
			return InheritanceType.JOINED;
		}
		else if ( JaxbUnionSubclassElement.class.isInstance( entityElement ) ) {
			return InheritanceType.TABLE_PER_CLASS;
		}
		else {
			return InheritanceType.NO_INHERITANCE;
		}
	}

	/**
	 * Given a user-specified description of how to perform custom SQL, build the {@link CustomSQL} representation.
	 *
	 * @param customSqlElement User-specified description of how to perform custom SQL
	 *
	 * @return The {@link CustomSQL} representation
	 */
	public static CustomSQL buildCustomSql(CustomSqlElement customSqlElement) {
		if ( customSqlElement == null ) {
			return null;
		}
		final ExecuteUpdateResultCheckStyle checkStyle = customSqlElement.getCheck() == null
				? customSqlElement.isCallable()
						? ExecuteUpdateResultCheckStyle.NONE
						: ExecuteUpdateResultCheckStyle.COUNT
				: ExecuteUpdateResultCheckStyle.fromExternalName( customSqlElement.getCheck().value() );
		return new CustomSQL( customSqlElement.getValue(), customSqlElement.isCallable(), checkStyle );
	}

	/**
	 * Given the user-specified entity mapping, determine the appropriate entity name
	 *
	 * @param entityElement The user-specified entity mapping
	 * @param unqualifiedClassPackage The package to use for unqualified class names
	 *
	 * @return The appropriate entity name
	 */
	public static String determineEntityName(EntityElement entityElement, String unqualifiedClassPackage) {
		return entityElement.getEntityName() != null
				? entityElement.getEntityName()
				: qualifyIfNeeded( entityElement.getName(), unqualifiedClassPackage );
	}

	/**
	 * Qualify a (supposed class) name with the unqualified-class package name if it is not already qualified
	 *
	 * @param name The name
	 * @param unqualifiedClassPackage The unqualified-class package name
	 *
	 * @return {@code null} if the incoming name was {@code null}; or the qualified name.
	 */
	public static String qualifyIfNeeded(String name, String unqualifiedClassPackage) {
		if ( name == null ) {
			return null;
		}
		if ( name.indexOf( '.' ) < 0 && unqualifiedClassPackage != null ) {
			return unqualifiedClassPackage + '.' + name;
		}
		return name;
	}

	public static String getPropertyAccessorName(String access, boolean isEmbedded, String defaultAccess) {
		return getStringValue( access, isEmbedded ? "embedded" : defaultAccess );
	}

	public static MetaAttributeContext extractMetaAttributeContext(
			List<JaxbMetaElement> metaElementList,
			boolean onlyInheritable,
			MetaAttributeContext parentContext) {
		final MetaAttributeContext subContext = new MetaAttributeContext( parentContext );

		for ( JaxbMetaElement metaElement : metaElementList ) {
			if ( onlyInheritable & !metaElement.isInherit() ) {
				continue;
			}

			final String name = metaElement.getAttribute();
			final MetaAttribute inheritedMetaAttribute = parentContext.getMetaAttribute( name );
			MetaAttribute metaAttribute = subContext.getLocalMetaAttribute( name );
			if ( metaAttribute == null || metaAttribute == inheritedMetaAttribute ) {
				metaAttribute = new MetaAttribute( name );
				subContext.add( metaAttribute );
			}
			metaAttribute.addValue( metaElement.getValue() );
		}

		return subContext;
	}

	public static String getStringValue(String value, String defaultValue) {
		return value == null ? defaultValue : value;
	}

	public static int getIntValue(String value, int defaultValue) {
		return value == null ? defaultValue : Integer.parseInt( value );
	}

	public static long getLongValue(String value, long defaultValue) {
		return value == null ? defaultValue : Long.parseLong( value );
	}

	public static boolean getBooleanValue(Boolean value, boolean defaultValue) {
		return value == null ? defaultValue : value;
	}

	public static Iterable<CascadeStyle> interpretCascadeStyles(String cascades, LocalBindingContext bindingContext) {
		final Set<CascadeStyle> cascadeStyles = new HashSet<CascadeStyle>();
		if ( StringHelper.isEmpty( cascades ) ) {
			cascades = bindingContext.getMappingDefaults().getCascadeStyle();
		}
		for ( String cascade : StringHelper.split( ",", cascades ) ) {
			cascadeStyles.add( CascadeStyle.getCascadeStyle( cascade ) );
		}
		return cascadeStyles;
	}

	public static Map<String, String> extractParameters(List<JaxbParamElement> xmlParamElements) {
		if ( xmlParamElements == null || xmlParamElements.isEmpty() ) {
			return null;
		}
		final HashMap<String,String> params = new HashMap<String, String>();
		for ( JaxbParamElement paramElement : xmlParamElements ) {
			params.put( paramElement.getName(), paramElement.getValue() );
		}
		return params;
	}

	public static Iterable<MetaAttributeSource> buildMetaAttributeSources(List<JaxbMetaElement> metaElements) {
		ArrayList<MetaAttributeSource> result = new ArrayList<MetaAttributeSource>();
		if ( metaElements == null || metaElements.isEmpty() ) {
			// do nothing
		}
		else {
			for ( final JaxbMetaElement metaElement : metaElements ) {
				result.add(
						new MetaAttributeSource() {
							@Override
							public String getName() {
								return metaElement.getAttribute();
							}

							@Override
							public String getValue() {
								return metaElement.getValue();
							}

							@Override
							public boolean isInheritable() {
								return metaElement.isInherit();
							}
						}
				);
			}
		}
		return result;
	}

	public static Schema.Name determineDatabaseSchemaName(
			String explicitSchemaName,
			String explicitCatalogName,
			LocalBindingContext bindingContext) {
		return new Schema.Name(
				resolveIdentifier(
						explicitSchemaName,
						bindingContext.getMappingDefaults().getSchemaName(),
						bindingContext.isGloballyQuotedIdentifiers()
				),
				resolveIdentifier(
						explicitCatalogName,
						bindingContext.getMappingDefaults().getCatalogName(),
						bindingContext.isGloballyQuotedIdentifiers()
				)
		);
	}

	public static Identifier resolveIdentifier(String explicitName, String defaultName, boolean globalQuoting) {
		String name = StringHelper.isNotEmpty( explicitName ) ? explicitName : defaultName;
		if ( globalQuoting ) {
			name = StringHelper.quote( name );
		}
		return Identifier.toIdentifier( name );
	}

	/**
	 * Operates like SQL coalesce expression, except empty strings are treated as null.  Return the first non-empty value
	 *
	 * @param values The list of values.
	 * @param <T>
	 *
	 * @return The first non-empty value, or null if all values were empty
	 */
	public static <T> T coalesce(T... values) {
		if ( values == null ) {
			return null;
		}
		for ( T value : values ) {
			if ( value != null ) {
				if ( String.class.isInstance( value ) ) {
					if ( StringHelper.isNotEmpty( (String) value ) ) {
						return value;
					}
				}
				else {
					return value;
				}
			}
		}
		return null;
	}

	/**
	 * For things that define one or more "value sources" there is a lot of variance in terms of how they
	 * look in the XML.  As an example, consider {@code <property/>} which might have:<ul>
	 *     <li>a {@code column} XML attribute</li>
	 *     <li>a {@code formula} XML attribute</li>
	 *     <li>one or more nested {@code <column/>} XML elements</li>
	 *     <li>a {@code <formula/>} XML element</li>
	 * </ul>
	 * as opposed to {@code <timestamp/>} which can only have:<ul>
	 *     <li>a {@code column} XML attribute</li>
	 * </ul>
	 * <p/>
	 * This class adapts those variances to a unified contract which is intended to be used in conjunction with
	 * {@link #buildValueSources}.
	 */
    public static class ValueSourcesAdapter {
        public String getContainingTableName() {
            return null;
        }

        public boolean isIncludedInInsertByDefault() {
            return false;
        }

        public boolean isIncludedInUpdateByDefault() {
            return false;
        }

        public String getColumnAttribute() {
            return null;
        }

        public String getFormulaAttribute() {
            return null;
        }

        public List getColumnOrFormulaElements() {
            return null;
        }

        public boolean isForceNotNull() {
            return false;
        }
    }

	/**
	 * Given a {@link ValueSourcesAdapter}, build the corresponding list of {@link RelationalValueSource}
	 *
	 * @param valueSourcesAdapter The adapter describing the "value sources.
	 * @param bindingContext The HBM binding context
	 *
	 * @return The corresponding list.
	 */
    public static List<RelationalValueSource> buildValueSources(
			ValueSourcesAdapter valueSourcesAdapter,
			LocalBindingContext bindingContext) {
		List<RelationalValueSource> result = new ArrayList<RelationalValueSource>();

		if ( StringHelper.isNotEmpty( valueSourcesAdapter.getColumnAttribute() ) ) {
			// we have the XML defining a column attribute.
			//		it is therefore illegal for there to also be any nested formula or column elements
			if ( valueSourcesAdapter.getColumnOrFormulaElements() != null
					&& ! valueSourcesAdapter.getColumnOrFormulaElements().isEmpty() ) {
				throw new MappingException(
						"column/formula attribute may not be used together with <column>/<formula> subelement",
						bindingContext.getOrigin()
				);
			}
			//		it is also illegal for there to also be a formula attribute
			if ( StringHelper.isNotEmpty( valueSourcesAdapter.getFormulaAttribute() ) ) {
				throw new MappingException(
						"column and formula attributes may not be used together",
						bindingContext.getOrigin()
				);
			}

			result.add(
					new ColumnAttributeSourceImpl(
							valueSourcesAdapter.getContainingTableName(),
							valueSourcesAdapter.getColumnAttribute(),
							valueSourcesAdapter.isIncludedInInsertByDefault(),
							valueSourcesAdapter.isIncludedInUpdateByDefault(),
                            valueSourcesAdapter.isForceNotNull()
					)
			);
		}
		else if ( StringHelper.isNotEmpty( valueSourcesAdapter.getFormulaAttribute() ) ) {
			// we have the XML defining a formula attribute (and not a column attribute)
			//		it is therefore illegal for there to also be any nested formula or column elements
			if ( valueSourcesAdapter.getColumnOrFormulaElements() != null
					&& ! valueSourcesAdapter.getColumnOrFormulaElements().isEmpty() ) {
				throw new MappingException(
						"column/formula attribute may not be used together with <column>/<formula> subelement",
						bindingContext.getOrigin()
				);
			}
			// 		column/formula attribute combo checked already

			result.add(
					new FormulaImpl(
							valueSourcesAdapter.getContainingTableName(),
							valueSourcesAdapter.getFormulaAttribute()
					)
			);
		}
		else if ( valueSourcesAdapter.getColumnOrFormulaElements() != null
				&& ! valueSourcesAdapter.getColumnOrFormulaElements().isEmpty() ) {
			// we have the XML defining nested formula or column elements (and not column attribute nor formula attribute)
			for ( Object columnOrFormulaElement : valueSourcesAdapter.getColumnOrFormulaElements() ) {
				if ( JaxbColumnElement.class.isInstance( columnOrFormulaElement ) ) {
					result.add(
							new ColumnSourceImpl(
									valueSourcesAdapter.getContainingTableName(),
									(JaxbColumnElement) columnOrFormulaElement,
									valueSourcesAdapter.isIncludedInInsertByDefault(),
									valueSourcesAdapter.isIncludedInUpdateByDefault(),
                                    valueSourcesAdapter.isForceNotNull()
							)
					);
				}
				else {
					result.add(
							new FormulaImpl(
									valueSourcesAdapter.getContainingTableName(),
									(String) columnOrFormulaElement
							)
					);
				}
			}
		}
		return result;
	}
}
