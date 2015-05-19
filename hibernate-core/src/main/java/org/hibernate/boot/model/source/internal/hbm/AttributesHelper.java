/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import javax.xml.bind.JAXBElement;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmAnyAssociationType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmArrayType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBagCollectionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmColumnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeKeyBasicAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeKeyManyToOneType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmDynamicComponentType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmIdBagCollectionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmListType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToOneType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmMapType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNestedCompositeElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOneToOneType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmPrimitiveArrayType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmPropertiesType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSetType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmToolingHintType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmTuplizerType;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.AttributeRole;
import org.hibernate.boot.model.source.spi.AttributeSource;
import org.hibernate.boot.model.source.spi.AttributeSourceContainer;
import org.hibernate.boot.model.source.spi.EmbeddableMapping;
import org.hibernate.boot.model.source.spi.EmbeddedAttributeMapping;
import org.hibernate.boot.model.source.spi.NaturalIdMutability;
import org.hibernate.boot.model.source.spi.SingularAttributeSourceEmbedded;
import org.hibernate.boot.model.source.spi.ToolingHintContext;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public class AttributesHelper {
	public static interface Callback {
		AttributeSourceContainer getAttributeSourceContainer();
		void addAttributeSource(AttributeSource attributeSource);

		void registerIndexColumn(String constraintName, String logicalTableName, String columnName);
		void registerUniqueKeyColumn(String constraintName, String logicalTableName, String columnName);
	}

	public static void processAttributes(
			MappingDocument mappingDocument,
			Callback callback,
			List attributeMappings,
			String logicalTableName,
			NaturalIdMutability naturalIdMutability) {
		for ( Object rawAttributeMapping : attributeMappings ) {
			processAttribute(
					mappingDocument,
					callback,
					rawAttributeMapping,
					logicalTableName,
					naturalIdMutability
			);
		}
	}


	private static void processAttribute(
			MappingDocument mappingDocument,
			Callback callback,
			Object attributeJaxbMapping,
			String logicalTableName,
			NaturalIdMutability naturalIdMutability) {
		if ( JAXBElement.class.isInstance( attributeJaxbMapping ) ) {
			processAttribute(
					mappingDocument,
					callback,
					( (JAXBElement) attributeJaxbMapping ).getValue(),
					logicalTableName,
					naturalIdMutability
			);
		}
		else if ( JaxbHbmCompositeKeyBasicAttributeType.class.isInstance( attributeJaxbMapping ) ) {
			callback.addAttributeSource(
					new CompositeIdentifierSingularAttributeSourceBasicImpl(
							mappingDocument,
							callback.getAttributeSourceContainer(),
							(JaxbHbmCompositeKeyBasicAttributeType) attributeJaxbMapping
					)
			);
		}
		else if ( JaxbHbmCompositeKeyManyToOneType.class.isInstance( attributeJaxbMapping ) ) {
			callback.addAttributeSource(
					new CompositeIdentifierSingularAttributeSourceManyToOneImpl(
							mappingDocument,
							callback.getAttributeSourceContainer(),
							(JaxbHbmCompositeKeyManyToOneType) attributeJaxbMapping
					)
			);
		}
		else if ( JaxbHbmPropertiesType.class.isInstance( attributeJaxbMapping ) ) {
			processPropertiesGroup(
					mappingDocument,
					callback,
					(JaxbHbmPropertiesType) attributeJaxbMapping,
					logicalTableName,
					naturalIdMutability
			);
		}
		else if ( JaxbHbmBasicAttributeType.class.isInstance( attributeJaxbMapping ) ) {
			processBasicAttribute(
					mappingDocument,
					callback,
					(JaxbHbmBasicAttributeType) attributeJaxbMapping,
					logicalTableName,
					naturalIdMutability
			);
		}
		else if ( JaxbHbmCompositeAttributeType.class.isInstance( attributeJaxbMapping ) ) {
			processEmbeddedAttribute(
					mappingDocument,
					callback,
					(JaxbHbmCompositeAttributeType) attributeJaxbMapping,
					logicalTableName,
					naturalIdMutability
			);
		}
		else if ( JaxbHbmDynamicComponentType.class.isInstance( attributeJaxbMapping ) ) {
			processDynamicComponentAttribute(
					mappingDocument,
					callback,
					(JaxbHbmDynamicComponentType) attributeJaxbMapping,
					logicalTableName,
					naturalIdMutability
			);
		}
		else if ( JaxbHbmManyToOneType.class.isInstance( attributeJaxbMapping ) ) {
			processManyToOneAttribute(
					mappingDocument,
					callback,
					(JaxbHbmManyToOneType) attributeJaxbMapping,
					logicalTableName,
					naturalIdMutability
			);
		}
		else if ( JaxbHbmOneToOneType.class.isInstance( attributeJaxbMapping ) ) {
			processOneToOneAttribute(
					mappingDocument,
					callback,
					(JaxbHbmOneToOneType) attributeJaxbMapping,
					logicalTableName,
					naturalIdMutability
			);
		}
		else if ( JaxbHbmAnyAssociationType.class.isInstance( attributeJaxbMapping ) ) {
			processAnyAttribute(
					mappingDocument,
					callback,
					(JaxbHbmAnyAssociationType) attributeJaxbMapping,
					logicalTableName,
					naturalIdMutability
			);
		}
		else if ( JaxbHbmMapType.class.isInstance( attributeJaxbMapping ) ) {
			processMapAttribute(
					mappingDocument,
					callback,
					(JaxbHbmMapType) attributeJaxbMapping
			);
		}
		else if ( JaxbHbmListType.class.isInstance( attributeJaxbMapping ) ) {
			processListAttribute(
					mappingDocument,
					callback,
					(JaxbHbmListType) attributeJaxbMapping
			);
		}
		else if ( JaxbHbmArrayType.class.isInstance( attributeJaxbMapping ) ) {
			processArrayAttribute(
					mappingDocument,
					callback,
					(JaxbHbmArrayType) attributeJaxbMapping
			);
		}
		else if ( JaxbHbmPrimitiveArrayType.class.isInstance( attributeJaxbMapping ) ) {
			processPrimitiveArrayAttribute(
					mappingDocument,
					callback,
					(JaxbHbmPrimitiveArrayType) attributeJaxbMapping
			);
		}
		else if ( JaxbHbmSetType.class.isInstance( attributeJaxbMapping ) ) {
			processSetAttribute(
					mappingDocument,
					callback,
					(JaxbHbmSetType) attributeJaxbMapping
			);
		}
		else if ( JaxbHbmBagCollectionType.class.isInstance( attributeJaxbMapping ) ) {
			processBagAttribute(
					mappingDocument,
					callback,
					(JaxbHbmBagCollectionType) attributeJaxbMapping
			);
		}
		else if ( JaxbHbmIdBagCollectionType.class.isInstance( attributeJaxbMapping ) ) {
			processIdBagAttribute(
					mappingDocument,
					callback,
					(JaxbHbmIdBagCollectionType) attributeJaxbMapping
			);
		}
		else if ( JaxbHbmNestedCompositeElementType.class.isInstance( attributeJaxbMapping ) ) {
			processNestedEmbeddedElement(
					mappingDocument,
					callback,
					(JaxbHbmNestedCompositeElementType) attributeJaxbMapping,
					logicalTableName,
					naturalIdMutability
			);
		}
		else {
			throw new MappingException(
					"Encountered unexpected JAXB mapping type for attribute : " + attributeJaxbMapping.getClass().getName(),
					mappingDocument.getOrigin()
			);
		}
	}

	public static void processCompositeKeySubAttributes(
			MappingDocument mappingDocument,
			Callback callback,
			List<?> jaxbAttributeMappings) {
		for ( Object jaxbAttributeMapping : jaxbAttributeMappings ) {
			if ( JaxbHbmCompositeKeyBasicAttributeType.class.isInstance( jaxbAttributeMapping ) ) {
				callback.addAttributeSource(
						new CompositeIdentifierSingularAttributeSourceBasicImpl(
								mappingDocument,
								callback.getAttributeSourceContainer(),
								(JaxbHbmCompositeKeyBasicAttributeType) jaxbAttributeMapping
						)
				);
			}
			else if ( JaxbHbmCompositeKeyManyToOneType.class.isInstance( jaxbAttributeMapping ) ) {
				callback.addAttributeSource(
						new CompositeIdentifierSingularAttributeSourceManyToOneImpl(
								mappingDocument,
								callback.getAttributeSourceContainer(),
								(JaxbHbmCompositeKeyManyToOneType) jaxbAttributeMapping
						)
				);
			}
			else {
				throw new MappingException(
						"Unexpected composite-key sub-attribute type : " + jaxbAttributeMapping.getClass().getName(),
						mappingDocument.getOrigin()
				);
			}
		}
	}

	private static void processPropertiesGroup(
			final MappingDocument mappingDocument,
			final Callback callback,
			final JaxbHbmPropertiesType propertiesGroupJaxbMapping,
			String logicalTableName,
			NaturalIdMutability naturalIdMutability) {
		final String name = propertiesGroupJaxbMapping.getName();
		final AttributeRole attributeRole = callback.getAttributeSourceContainer()
				.getAttributeRoleBase()
				.append( name );
		final AttributePath attributePath = callback.getAttributeSourceContainer()
				.getAttributePathBase()
				.append( name );

		final EmbeddableSourceVirtualImpl embeddable = new EmbeddableSourceVirtualImpl(
				mappingDocument,
				callback,
				new EmbeddableSourceContainer() {
					@Override
					public AttributeRole getAttributeRoleBase() {
						return attributeRole;
					}

					@Override
					public AttributePath getAttributePathBase() {
						return attributePath;
					}

					@Override
					public ToolingHintContext getToolingHintContextBaselineForEmbeddable() {
						return callback.getAttributeSourceContainer().getToolingHintContext();
					}

					@Override
					public void registerIndexConstraintColumn(
							String constraintName,
							String logicalTableName,
							String columnName) {
						callback.registerIndexColumn( constraintName, logicalTableName, columnName );
					}

					@Override
					public void registerUniqueKeyConstraintColumn(
							String constraintName,
							String logicalTableName,
							String columnName) {
						callback.registerUniqueKeyColumn( constraintName, logicalTableName, columnName );
					}
				},
				propertiesGroupJaxbMapping.getAttributes(),
				logicalTableName,
				naturalIdMutability,
				propertiesGroupJaxbMapping
		);

		// fake the JAXB mapping...
		final EmbeddableMapping embeddableMapping = new EmbeddableMapping() {
			@Override
			public String getClazz() {
				return null;
			}

			@Override
			public List<JaxbHbmTuplizerType> getTuplizer() {
				return Collections.emptyList();
			}

			@Override
			public String getParent() {
				return null;
			}
		};

		final EmbeddedAttributeMapping attributeMapping = new EmbeddedAttributeMapping() {
			@Override
			public boolean isUnique() {
				return propertiesGroupJaxbMapping.isUnique();
			}

			@Override
			public EmbeddableMapping getEmbeddableMapping() {
				return embeddableMapping;
			}

			@Override
			public String getName() {
				return propertiesGroupJaxbMapping.getName();
			}

			@Override
			public String getAccess() {
				return null;
			}

			@Override
			public List<JaxbHbmToolingHintType> getToolingHints() {
				return Collections.emptyList();
			}
		};

		// todo : make the virtual embedded attribute
		final SingularAttributeSourceEmbedded virtualAttribute = new AbstractSingularAttributeSourceEmbeddedImpl(
				mappingDocument,
				attributeMapping,
				embeddable,
				naturalIdMutability
		) {
			@Override
			public boolean isVirtualAttribute() {
				return true;
			}

			@Override
			public Boolean isInsertable() {
				return propertiesGroupJaxbMapping.isInsert();
			}

			@Override
			public Boolean isUpdatable() {
				return propertiesGroupJaxbMapping.isUpdate();
			}

			@Override
			public boolean isBytecodeLazy() {
				return false;
			}

			@Override
			public XmlElementMetadata getSourceType() {
				return XmlElementMetadata.PROPERTIES;
			}

			@Override
			public String getXmlNodeName() {
				return null;
			}

			@Override
			public AttributePath getAttributePath() {
				return attributePath;
			}

			@Override
			public AttributeRole getAttributeRole() {
				return attributeRole;
			}

			@Override
			public boolean isIncludedInOptimisticLocking() {
				return false;
			}

			@Override
			public ToolingHintContext getToolingHintContext() {
				return mappingDocument.getToolingHintContext();
			}
		};

		callback.addAttributeSource( virtualAttribute );
	}

	public static void processBasicAttribute(
			MappingDocument mappingDocument,
			Callback callback,
			JaxbHbmBasicAttributeType basicAttributeJaxbMapping,
			String logicalTableName,
			NaturalIdMutability naturalIdMutability) {
		callback.addAttributeSource(
				new SingularAttributeSourceBasicImpl(
						mappingDocument,
						callback.getAttributeSourceContainer(),
						basicAttributeJaxbMapping,
						logicalTableName,
						naturalIdMutability
				)
		);

		processConstraints(
				callback,
				logicalTableName,
				basicAttributeJaxbMapping.getColumnAttribute(),
				basicAttributeJaxbMapping.getColumnOrFormula(),
				basicAttributeJaxbMapping.getIndex(),
				basicAttributeJaxbMapping.getUniqueKey()
		);
	}

	private static void processConstraints(
			Callback callback,
			String logicalTableName,
			String columnAttribute,
			List columns,
			String groupedIndexNames,
			String groupedUniqueKeyNames) {

		final Set<String> groupedIndexNameSet = splitNames( groupedIndexNames );
		final boolean hasGroupedIndexes = !groupedIndexNameSet.isEmpty();

		final Set<String> groupedUniqueKeyNameSet = splitNames( groupedUniqueKeyNames );
		final boolean hasGroupedUniqueKeys = !groupedUniqueKeyNameSet.isEmpty();

		if ( hasGroupedIndexes && StringHelper.isNotEmpty( columnAttribute ) ) {
			for ( String name : groupedIndexNameSet ) {
				callback.registerIndexColumn( name, logicalTableName, columnAttribute );
			}
		}

		if ( hasGroupedUniqueKeys && StringHelper.isNotEmpty( columnAttribute ) ) {
			for ( String name : groupedUniqueKeyNameSet ) {
				callback.registerUniqueKeyColumn( name, logicalTableName, columnAttribute );
			}
		}

		for ( Object oColumn : columns ) {
			if ( !JaxbHbmColumnType.class.isInstance( oColumn ) ) {
				continue;
			}

			final JaxbHbmColumnType column = (JaxbHbmColumnType) oColumn;
			if ( StringHelper.isNotEmpty( column.getIndex() ) ) {
				callback.registerIndexColumn( column.getIndex(), logicalTableName, column.getName() );
			}
			if ( hasGroupedIndexes ) {
				for ( String name : groupedIndexNameSet ) {
					callback.registerIndexColumn( name, logicalTableName, column.getName() );
				}
			}

			if ( StringHelper.isNotEmpty( column.getUniqueKey() ) ) {
				callback.registerUniqueKeyColumn( column.getUniqueKey(), logicalTableName, column.getName() );
			}
			if ( hasGroupedUniqueKeys ) {
				for ( String name : groupedUniqueKeyNameSet ) {
					callback.registerUniqueKeyColumn( name, logicalTableName, column.getName() );
				}
			}
		}
	}

	private static Set<String> splitNames(String groupedNames) {
		if ( StringHelper.isEmpty( groupedNames ) ) {
			return Collections.emptySet();
		}

		final HashSet<String> splitNames = new HashSet<String>();
		final StringTokenizer tokens = new StringTokenizer( groupedNames, ", " );
		while ( tokens.hasMoreTokens() ) {
			splitNames.add( tokens.nextToken() );
		}
		return splitNames;
	}

	public static void processEmbeddedAttribute(
			MappingDocument mappingDocument,
			Callback callback,
			JaxbHbmCompositeAttributeType embeddedAttributeJaxbMapping,
			String logicalTableName,
			NaturalIdMutability naturalIdMutability) {
		callback.addAttributeSource(
				new SingularAttributeSourceEmbeddedImpl(
						mappingDocument,
						callback.getAttributeSourceContainer(),
						embeddedAttributeJaxbMapping,
						naturalIdMutability,
						logicalTableName
				)
		);
	}

	private static void processNestedEmbeddedElement(
			MappingDocument mappingDocument,
			Callback callback,
			JaxbHbmNestedCompositeElementType attributeJaxbMapping,
			String logicalTableName,
			NaturalIdMutability naturalIdMutability) {
		callback.addAttributeSource(
				new SingularAttributeSourceEmbeddedImpl(
						mappingDocument,
						callback.getAttributeSourceContainer(),
						attributeJaxbMapping,
						naturalIdMutability,
						logicalTableName
				)
		);
	}

	public static void processDynamicComponentAttribute(
			MappingDocument mappingDocument,
			Callback callback,
			JaxbHbmDynamicComponentType dynamicComponentJaxbMapping,
			String logicalTableName,
			NaturalIdMutability naturalIdMutability) {
		callback.addAttributeSource(
				new SingularAttributeSourceEmbeddedImpl(
						mappingDocument,
						callback.getAttributeSourceContainer(),
						dynamicComponentJaxbMapping,
						naturalIdMutability,
						logicalTableName
				)
		);
	}

	public static void processManyToOneAttribute(
			MappingDocument mappingDocument,
			Callback callback,
			JaxbHbmManyToOneType manyToOneAttributeJaxbMapping,
			String logicalTableName,
			NaturalIdMutability naturalIdMutability) {
		callback.addAttributeSource(
				new SingularAttributeSourceManyToOneImpl(
						mappingDocument,
						callback.getAttributeSourceContainer(),
						manyToOneAttributeJaxbMapping,
						logicalTableName,
						naturalIdMutability
				)
		);

		processConstraints(
				callback,
				logicalTableName,
				manyToOneAttributeJaxbMapping.getColumnAttribute(),
				manyToOneAttributeJaxbMapping.getColumnOrFormula(),
				manyToOneAttributeJaxbMapping.getIndex(),
				manyToOneAttributeJaxbMapping.getUniqueKey()
		);
	}

	public static void processOneToOneAttribute(
			MappingDocument mappingDocument,
			Callback callback,
			JaxbHbmOneToOneType oneToOneAttributeJaxbMapping,
			String logicalTableName,
			NaturalIdMutability naturalIdMutability) {
		callback.addAttributeSource(
				new SingularAttributeSourceOneToOneImpl(
						mappingDocument,
						callback.getAttributeSourceContainer(),
						oneToOneAttributeJaxbMapping,
						logicalTableName,
						naturalIdMutability
				)
		);
	}

	public static void processAnyAttribute(
			MappingDocument mappingDocument,
			Callback callback,
			JaxbHbmAnyAssociationType anyAttributeJaxbMapping,
			String logicalTableName,
			NaturalIdMutability naturalIdMutability) {
		callback.addAttributeSource(
				new SingularAttributeSourceAnyImpl(
						mappingDocument,
						callback.getAttributeSourceContainer(),
						anyAttributeJaxbMapping,
						logicalTableName,
						naturalIdMutability
				)
		);

		processConstraints(
				callback,
				logicalTableName,
				null,
				// todo : should we skip the discriminator column?
				anyAttributeJaxbMapping.getColumn(),
				anyAttributeJaxbMapping.getIndex(),
				null
		);
	}

	public static void processMapAttribute(
			MappingDocument mappingDocument,
			Callback callback,
			JaxbHbmMapType mapAttributesJaxbMapping) {
		callback.addAttributeSource(
				new PluralAttributeSourceMapImpl(
						mappingDocument,
						mapAttributesJaxbMapping,
						callback.getAttributeSourceContainer()
				)
		);
	}

	public static void processListAttribute(
			MappingDocument mappingDocument,
			Callback callback,
			JaxbHbmListType listAttributeJaxbMapping) {
		callback.addAttributeSource(
				new PluralAttributeSourceListImpl(
						mappingDocument,
						listAttributeJaxbMapping,
						callback.getAttributeSourceContainer()
				)
		);
	}

	public static void processArrayAttribute(
			MappingDocument mappingDocument,
			Callback callback,
			JaxbHbmArrayType arrayAttributeJaxbMapping) {
		callback.addAttributeSource(
				new PluralAttributeSourceArrayImpl(
						mappingDocument,
						arrayAttributeJaxbMapping,
						callback.getAttributeSourceContainer()
				)
		);
	}

	public static void processPrimitiveArrayAttribute(
			MappingDocument mappingDocument,
			Callback callback,
			JaxbHbmPrimitiveArrayType primitiveArrayAttributeJaxbMapping) {
		callback.addAttributeSource(
				new PluralAttributeSourcePrimitiveArrayImpl(
						mappingDocument,
						primitiveArrayAttributeJaxbMapping,
						callback.getAttributeSourceContainer()
				)
		);
	}

	public static void processSetAttribute(
			MappingDocument mappingDocument,
			Callback callback,
			JaxbHbmSetType setAttributeJaxbMapping) {
		callback.addAttributeSource(
				new PluralAttributeSourceSetImpl(
						mappingDocument,
						setAttributeJaxbMapping,
						callback.getAttributeSourceContainer()
				)
		);
	}

	public static void processBagAttribute(
			MappingDocument mappingDocument,
			Callback callback,
			JaxbHbmBagCollectionType bagAttributeJaxbMapping) {
		callback.addAttributeSource(
				new PluralAttributeSourceBagImpl(
						mappingDocument,
						bagAttributeJaxbMapping,
						callback.getAttributeSourceContainer()
				)
		);
	}

	public static void processIdBagAttribute(
			MappingDocument mappingDocument,
			Callback callback,
			JaxbHbmIdBagCollectionType idBagAttributeJaxbMapping) {
		callback.addAttributeSource(
				new PluralAttributeSourceIdBagImpl(
						mappingDocument,
						idBagAttributeJaxbMapping,
						callback.getAttributeSourceContainer()
				)
		);
	}

}
