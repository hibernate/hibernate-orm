/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.models.builder.hbm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBagCollectionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmColumnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeIdType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmGeneratorSpecificationType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmListType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToManyCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToOneType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmMapType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOneToManyCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOneToOneType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmRootEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSetType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSimpleIdType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmVersionAttributeType;
import org.hibernate.boot.jaxb.hbm.transform.UnsupportedFeatureHandling;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.ColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EntityJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.GeneratedValueJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.IdJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ManyToOneJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OneToManyJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.VersionJpaAnnotation;
import org.hibernate.models.internal.BasicModelsContextImpl;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.internal.MutableClassDetailsRegistry;
import org.hibernate.models.internal.SimpleClassLoading;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassLoading;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.TypeDetails;

import jakarta.persistence.GenerationType;

/**
 * Builds dynamic entity {@link ClassDetails} with JPA annotations
 * from hbm.xml files using Hibernate ORM's JAXB-based
 * {@link MappingBinder} for parsing.
 * <p>
 * This avoids both the legacy {@code org.hibernate.mapping} API
 * and the {@code TRANSFORM_HBM_XML} mechanism which requires
 * entity classes on the classpath.
 *
 * @author Koen Aers
 */
public class HbmClassDetailsBuilder {

	private static final Map<String, String> HIBERNATE_TYPE_MAP = new HashMap<>();

	static {
		HIBERNATE_TYPE_MAP.put("string", "java.lang.String");
		HIBERNATE_TYPE_MAP.put("long", "java.lang.Long");
		HIBERNATE_TYPE_MAP.put("int", "java.lang.Integer");
		HIBERNATE_TYPE_MAP.put("integer", "java.lang.Integer");
		HIBERNATE_TYPE_MAP.put("short", "java.lang.Short");
		HIBERNATE_TYPE_MAP.put("byte", "java.lang.Byte");
		HIBERNATE_TYPE_MAP.put("float", "java.lang.Float");
		HIBERNATE_TYPE_MAP.put("double", "java.lang.Double");
		HIBERNATE_TYPE_MAP.put("boolean", "java.lang.Boolean");
		HIBERNATE_TYPE_MAP.put("yes_no", "java.lang.Boolean");
		HIBERNATE_TYPE_MAP.put("true_false", "java.lang.Boolean");
		HIBERNATE_TYPE_MAP.put("big_decimal", "java.math.BigDecimal");
		HIBERNATE_TYPE_MAP.put("big_integer", "java.math.BigInteger");
		HIBERNATE_TYPE_MAP.put("character", "java.lang.Character");
		HIBERNATE_TYPE_MAP.put("char", "java.lang.Character");
		HIBERNATE_TYPE_MAP.put("date", "java.util.Date");
		HIBERNATE_TYPE_MAP.put("time", "java.util.Date");
		HIBERNATE_TYPE_MAP.put("timestamp", "java.util.Date");
		HIBERNATE_TYPE_MAP.put("calendar", "java.util.Calendar");
		HIBERNATE_TYPE_MAP.put("calendar_date", "java.util.Calendar");
		HIBERNATE_TYPE_MAP.put("binary", "byte[]");
		HIBERNATE_TYPE_MAP.put("text", "java.lang.String");
		HIBERNATE_TYPE_MAP.put("clob", "java.sql.Clob");
		HIBERNATE_TYPE_MAP.put("blob", "java.sql.Blob");
		HIBERNATE_TYPE_MAP.put("serializable", "java.io.Serializable");
	}

	private final ModelsContext modelsContext;
	private final MappingBinder mappingBinder;

	public HbmClassDetailsBuilder() {
		ClassLoading classLoading = SimpleClassLoading.SIMPLE_CLASS_LOADING;
		this.modelsContext = new BasicModelsContextImpl(classLoading, false, null);
		this.mappingBinder = new MappingBinder(
				MappingBinder.class.getClassLoader()::getResourceAsStream,
				UnsupportedFeatureHandling.ERROR);
	}

	public ModelsContext getModelsContext() {
		return modelsContext;
	}

	/**
	 * Parses the given hbm.xml files using {@link MappingBinder} and builds
	 * {@link ClassDetails} with JPA annotations for each entity.
	 */
	public List<ClassDetails> buildFromFiles(File... hbmFiles) {
		List<ClassDetails> entities = new ArrayList<>();
		for (File file : hbmFiles) {
			JaxbHbmHibernateMapping mapping = parseHbmXml(file);
			String packageName = mapping.getPackage();
			for (JaxbHbmRootEntityType entityType : mapping.getClazz()) {
				entities.add(buildEntity(entityType, packageName));
			}
		}
		return entities;
	}

	private JaxbHbmHibernateMapping parseHbmXml(File hbmFile) {
		Origin origin = new Origin(SourceType.FILE, hbmFile.getAbsolutePath());
		try (FileInputStream stream = new FileInputStream(hbmFile)) {
			Binding<JaxbHbmHibernateMapping> binding = mappingBinder.bind(stream, origin);
			return binding.getRoot();
		} catch (IOException e) {
			throw new RuntimeException(
					"Failed to parse hbm.xml file: " + hbmFile.getAbsolutePath(), e);
		}
	}

	private ClassDetails buildEntity(JaxbHbmRootEntityType entityType, String defaultPackage) {
		String className = entityType.getName();
		String fullName = resolveClassName(className, defaultPackage);
		String simpleName = simpleName(fullName);

		DynamicClassDetails entityClass = new DynamicClassDetails(
				simpleName, fullName, Object.class,
				false, null, null, modelsContext);

		addEntityAnnotation(entityClass, simpleName);
		addTableAnnotation(entityClass, entityType);
		processId(entityClass, entityType, defaultPackage);
		processCompositeId(entityClass, entityType, defaultPackage);
		processVersion(entityClass, entityType);
		processAttributes(entityClass, entityType, defaultPackage);

		registerClassDetails(entityClass);
		return entityClass;
	}

	private void addEntityAnnotation(DynamicClassDetails entityClass, String entityName) {
		EntityJpaAnnotation entityAnnotation = JpaAnnotations.ENTITY.createUsage(modelsContext);
		entityAnnotation.name(entityName);
		entityClass.addAnnotationUsage(entityAnnotation);
	}

	private void addTableAnnotation(DynamicClassDetails entityClass, JaxbHbmRootEntityType entityType) {
		String tableName = entityType.getTable();
		if (tableName != null && !tableName.isEmpty()) {
			TableJpaAnnotation tableAnnotation = JpaAnnotations.TABLE.createUsage(modelsContext);
			tableAnnotation.name(tableName);
			String schema = entityType.getSchema();
			if (schema != null && !schema.isEmpty()) {
				tableAnnotation.schema(schema);
			}
			String catalog = entityType.getCatalog();
			if (catalog != null && !catalog.isEmpty()) {
				tableAnnotation.catalog(catalog);
			}
			entityClass.addAnnotationUsage(tableAnnotation);
		}
	}

	private void processId(DynamicClassDetails entityClass, JaxbHbmRootEntityType entityType,
						    String defaultPackage) {
		JaxbHbmSimpleIdType id = entityType.getId();
		if (id == null) {
			return;
		}
		String name = id.getName();
		String typeName = resolveIdType(id);
		String javaType = resolveJavaType(typeName);

		DynamicFieldDetails field = createField(entityClass, name, javaType);

		// @Id
		IdJpaAnnotation idAnnotation = JpaAnnotations.ID.createUsage(modelsContext);
		field.addAnnotationUsage(idAnnotation);

		// @GeneratedValue
		JaxbHbmGeneratorSpecificationType generator = id.getGenerator();
		if (generator != null) {
			String genClass = generator.getClazz();
			GenerationType genType = mapGeneratorClass(genClass);
			if (genType != null) {
				GeneratedValueJpaAnnotation genAnnotation =
						JpaAnnotations.GENERATED_VALUE.createUsage(modelsContext);
				genAnnotation.strategy(genType);
				field.addAnnotationUsage(genAnnotation);
			}
		}

		// @Column
		addColumnAnnotation(field, id.getColumn(), id.getColumnAttribute(), name);
	}

	private void processCompositeId(DynamicClassDetails entityClass,
									JaxbHbmRootEntityType entityType,
									String defaultPackage) {
		JaxbHbmCompositeIdType compositeId = entityType.getCompositeId();
		if (compositeId == null) {
			return;
		}
		// Process key properties as @Id fields
		for (Object keyProp : compositeId.getKeyPropertyOrKeyManyToOne()) {
			if (keyProp instanceof org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeKeyBasicAttributeType keyAttr) {
				String name = keyAttr.getName();
				String typeName = keyAttr.getTypeAttribute();
				String javaType = resolveJavaType(typeName != null ? typeName : "string");

				DynamicFieldDetails field = createField(entityClass, name, javaType);
				IdJpaAnnotation idAnnotation = JpaAnnotations.ID.createUsage(modelsContext);
				field.addAnnotationUsage(idAnnotation);
				addColumnAnnotation(field, keyAttr.getColumn(), keyAttr.getColumnAttribute(), name);
			} else if (keyProp instanceof org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeKeyManyToOneType keyM2o) {
				String name = keyM2o.getName();
				String targetClassName = keyM2o.getClazz();
				String fullTargetName = resolveClassName(targetClassName, defaultPackage);
				ClassDetails targetClass = resolveOrCreateClassDetails(
						simpleName(fullTargetName), fullTargetName);

				TypeDetails fieldType = new ClassTypeDetailsImpl(
						targetClass, TypeDetails.Kind.CLASS);
				DynamicFieldDetails field = entityClass.applyAttribute(
						name, fieldType, false, false, modelsContext);

				IdJpaAnnotation idAnnotation = JpaAnnotations.ID.createUsage(modelsContext);
				field.addAnnotationUsage(idAnnotation);
				ManyToOneJpaAnnotation m2oAnnotation =
						JpaAnnotations.MANY_TO_ONE.createUsage(modelsContext);
				field.addAnnotationUsage(m2oAnnotation);
			}
		}
	}

	private void processVersion(DynamicClassDetails entityClass,
								JaxbHbmRootEntityType entityType) {
		JaxbHbmVersionAttributeType version = entityType.getVersion();
		if (version == null) {
			return;
		}
		String name = version.getName();
		String typeName = version.getType();
		String javaType = resolveJavaType(typeName != null ? typeName : "integer");

		DynamicFieldDetails field = createField(entityClass, name, javaType);
		VersionJpaAnnotation versionAnnotation =
				JpaAnnotations.VERSION.createUsage(modelsContext);
		field.addAnnotationUsage(versionAnnotation);
		addColumnAnnotation(field, version.getColumn(), version.getColumnAttribute(), name);
	}

	private void processAttributes(DynamicClassDetails entityClass,
								   JaxbHbmRootEntityType entityType,
								   String defaultPackage) {
		for (Serializable attribute : entityType.getAttributes()) {
			if (attribute instanceof JaxbHbmBasicAttributeType basicAttr) {
				processProperty(entityClass, basicAttr);
			} else if (attribute instanceof JaxbHbmManyToOneType m2o) {
				processManyToOne(entityClass, m2o, defaultPackage);
			} else if (attribute instanceof JaxbHbmOneToOneType o2o) {
				processOneToOne(entityClass, o2o, defaultPackage);
			} else if (attribute instanceof JaxbHbmSetType set) {
				processSetCollection(entityClass, set, defaultPackage);
			} else if (attribute instanceof JaxbHbmListType list) {
				processListCollection(entityClass, list, defaultPackage);
			} else if (attribute instanceof JaxbHbmBagCollectionType bag) {
				processBagCollection(entityClass, bag, defaultPackage);
			} else if (attribute instanceof JaxbHbmMapType map) {
				processMapCollection(entityClass, map, defaultPackage);
			}
		}
	}

	private void processProperty(DynamicClassDetails entityClass,
								 JaxbHbmBasicAttributeType basicAttr) {
		String name = basicAttr.getName();
		String typeName = basicAttr.getTypeAttribute();
		String javaType = resolveJavaType(typeName != null ? typeName : "string");

		DynamicFieldDetails field = createField(entityClass, name, javaType);
		addColumnAnnotationFromBasicAttr(field, basicAttr);
	}

	private void processManyToOne(DynamicClassDetails entityClass,
								  JaxbHbmManyToOneType m2o, String defaultPackage) {
		String name = m2o.getName();
		String targetClassName = m2o.getClazz();
		String fullTargetName = resolveClassName(targetClassName, defaultPackage);
		ClassDetails targetClass = resolveOrCreateClassDetails(
				simpleName(fullTargetName), fullTargetName);

		TypeDetails fieldType = new ClassTypeDetailsImpl(
				targetClass, TypeDetails.Kind.CLASS);
		DynamicFieldDetails field = entityClass.applyAttribute(
				name, fieldType, false, false, modelsContext);

		ManyToOneJpaAnnotation m2oAnnotation =
				JpaAnnotations.MANY_TO_ONE.createUsage(modelsContext);
		field.addAnnotationUsage(m2oAnnotation);

		String columnName = m2o.getColumnAttribute();
		if (columnName != null && !columnName.isEmpty()) {
			JoinColumnJpaAnnotation joinColAnnotation =
					JpaAnnotations.JOIN_COLUMN.createUsage(modelsContext);
			joinColAnnotation.name(columnName);
			field.addAnnotationUsage(joinColAnnotation);
		}
	}

	private void processOneToOne(DynamicClassDetails entityClass,
								 JaxbHbmOneToOneType o2o, String defaultPackage) {
		String name = o2o.getName();
		String targetClassName = o2o.getClazz();
		String fullTargetName = resolveClassName(targetClassName, defaultPackage);
		ClassDetails targetClass = resolveOrCreateClassDetails(
				simpleName(fullTargetName), fullTargetName);

		TypeDetails fieldType = new ClassTypeDetailsImpl(
				targetClass, TypeDetails.Kind.CLASS);
		entityClass.applyAttribute(name, fieldType, false, false, modelsContext);
	}

	// --- Collection processing ---

	private void processSetCollection(DynamicClassDetails entityClass,
									  JaxbHbmSetType set, String defaultPackage) {
		processCollectionElement(entityClass, set.getName(),
				set.getOneToMany(), set.getManyToMany(), defaultPackage);
	}

	private void processListCollection(DynamicClassDetails entityClass,
									   JaxbHbmListType list, String defaultPackage) {
		processCollectionElement(entityClass, list.getName(),
				list.getOneToMany(), list.getManyToMany(), defaultPackage);
	}

	private void processBagCollection(DynamicClassDetails entityClass,
									  JaxbHbmBagCollectionType bag, String defaultPackage) {
		processCollectionElement(entityClass, bag.getName(),
				bag.getOneToMany(), bag.getManyToMany(), defaultPackage);
	}

	private void processMapCollection(DynamicClassDetails entityClass,
									  JaxbHbmMapType map, String defaultPackage) {
		processCollectionElement(entityClass, map.getName(),
				map.getOneToMany(), map.getManyToMany(), defaultPackage);
	}

	private void processCollectionElement(DynamicClassDetails entityClass,
										  String name,
										  JaxbHbmOneToManyCollectionElementType oneToMany,
										  JaxbHbmManyToManyCollectionElementType manyToMany,
										  String defaultPackage) {
		String targetClassName = null;
		if (oneToMany != null) {
			targetClassName = oneToMany.getClazz();
		} else if (manyToMany != null) {
			targetClassName = manyToMany.getClazz();
		}
		if (targetClassName == null) {
			return;
		}
		String fullTargetName = resolveClassName(targetClassName, defaultPackage);
		ClassDetails targetClass = resolveOrCreateClassDetails(
				simpleName(fullTargetName), fullTargetName);

		TypeDetails fieldType = new ClassTypeDetailsImpl(
				targetClass, TypeDetails.Kind.CLASS);
		DynamicFieldDetails field = entityClass.applyAttribute(
				name, fieldType, false, true, modelsContext);

		OneToManyJpaAnnotation o2mAnnotation =
				JpaAnnotations.ONE_TO_MANY.createUsage(modelsContext);
		field.addAnnotationUsage(o2mAnnotation);
	}

	// --- Column annotation helpers ---

	private void addColumnAnnotationFromBasicAttr(DynamicFieldDetails field,
												  JaxbHbmBasicAttributeType basicAttr) {
		String columnName = basicAttr.getColumnAttribute();
		boolean notNull = Boolean.TRUE.equals(basicAttr.isNotNull());
		int length = basicAttr.getLength() != null ? basicAttr.getLength() : 0;
		String precisionStr = basicAttr.getPrecision();
		int precision = precisionStr != null && !precisionStr.isEmpty()
				? Integer.parseInt(precisionStr) : 0;
		String scaleStr = basicAttr.getScale();
		int scale = scaleStr != null && !scaleStr.isEmpty()
				? Integer.parseInt(scaleStr) : 0;
		boolean unique = basicAttr.isUnique();

		// Check nested column elements via getColumnOrFormula()
		for (Serializable colOrFormula : basicAttr.getColumnOrFormula()) {
			if (colOrFormula instanceof JaxbHbmColumnType col) {
				if (col.getName() != null) {
					columnName = col.getName();
				}
				if (Boolean.TRUE.equals(col.isNotNull())) {
					notNull = true;
				}
				if (col.getLength() != null) {
					length = col.getLength();
				}
				if (col.getPrecision() != null) {
					precision = col.getPrecision();
				}
				if (col.getScale() != null) {
					scale = col.getScale();
				}
				if (Boolean.TRUE.equals(col.isUnique())) {
					unique = true;
				}
				break;
			}
		}

		if (columnName == null || columnName.isEmpty()) {
			columnName = field.getName();
		}

		ColumnJpaAnnotation columnAnnotation = JpaAnnotations.COLUMN.createUsage(modelsContext);
		columnAnnotation.name(columnName);
		columnAnnotation.nullable(!notNull);
		if (length > 0) {
			columnAnnotation.length(length);
		}
		if (precision > 0) {
			columnAnnotation.precision(precision);
		}
		if (scale > 0) {
			columnAnnotation.scale(scale);
		}
		if (unique) {
			columnAnnotation.unique(true);
		}
		field.addAnnotationUsage(columnAnnotation);
	}

	private void addColumnAnnotation(DynamicFieldDetails field,
									 List<JaxbHbmColumnType> columns,
									 String columnAttribute,
									 String defaultColumnName) {
		String columnName = columnAttribute;
		boolean notNull = false;
		int length = 0;
		int precision = 0;
		int scale = 0;
		boolean unique = false;

		if (columns != null && !columns.isEmpty()) {
			JaxbHbmColumnType col = columns.get(0);
			if (col.getName() != null) {
				columnName = col.getName();
			}
			if (Boolean.TRUE.equals(col.isNotNull())) {
				notNull = true;
			}
			if (col.getLength() != null) {
				length = col.getLength();
			}
			if (col.getPrecision() != null) {
				precision = col.getPrecision();
			}
			if (col.getScale() != null) {
				scale = col.getScale();
			}
			if (Boolean.TRUE.equals(col.isUnique())) {
				unique = true;
			}
		}

		if (columnName == null || columnName.isEmpty()) {
			columnName = defaultColumnName;
		}

		ColumnJpaAnnotation columnAnnotation = JpaAnnotations.COLUMN.createUsage(modelsContext);
		columnAnnotation.name(columnName);
		columnAnnotation.nullable(!notNull);
		if (length > 0) {
			columnAnnotation.length(length);
		}
		if (precision > 0) {
			columnAnnotation.precision(precision);
		}
		if (scale > 0) {
			columnAnnotation.scale(scale);
		}
		if (unique) {
			columnAnnotation.unique(true);
		}
		field.addAnnotationUsage(columnAnnotation);
	}

	// --- Type resolution ---

	private String resolveIdType(JaxbHbmSimpleIdType id) {
		String typeAttr = id.getTypeAttribute();
		if (typeAttr != null && !typeAttr.isEmpty()) {
			return typeAttr;
		}
		if (id.getType() != null && id.getType().getName() != null) {
			return id.getType().getName();
		}
		return "long";
	}

	private String resolveJavaType(String hibernateType) {
		if (hibernateType == null || hibernateType.isEmpty()) {
			return "java.lang.String";
		}
		String mapped = HIBERNATE_TYPE_MAP.get(hibernateType.toLowerCase());
		if (mapped != null) {
			return mapped;
		}
		if (hibernateType.contains(".")) {
			return hibernateType;
		}
		return "java.lang.String";
	}

	private GenerationType mapGeneratorClass(String generatorClass) {
		if (generatorClass == null || generatorClass.isEmpty()) {
			return null;
		}
		return switch (generatorClass) {
			case "identity", "native" -> GenerationType.IDENTITY;
			case "sequence", "seqhilo",
				 "enhanced-sequence", "org.hibernate.id.enhanced.SequenceStyleGenerator"
					-> GenerationType.SEQUENCE;
			case "enhanced-table", "org.hibernate.id.enhanced.TableGenerator"
					-> GenerationType.TABLE;
			case "uuid", "uuid2", "guid" -> GenerationType.UUID;
			case "assigned" -> null;
			default -> GenerationType.AUTO;
		};
	}

	// --- Class name resolution ---

	private static String resolveClassName(String name, String defaultPackage) {
		if (name == null || name.isEmpty()) {
			return name;
		}
		if (name.contains(".")) {
			return name;
		}
		if (defaultPackage != null && !defaultPackage.isEmpty()) {
			return defaultPackage + "." + name;
		}
		return name;
	}

	private static String simpleName(String fullName) {
		int lastDot = fullName.lastIndexOf('.');
		return lastDot > 0 ? fullName.substring(lastDot + 1) : fullName;
	}

	// --- ClassDetails registry helpers ---

	private DynamicFieldDetails createField(DynamicClassDetails entityClass,
											String fieldName, String javaType) {
		ClassDetails fieldTypeClass = modelsContext.getClassDetailsRegistry()
				.resolveClassDetails(javaType);
		TypeDetails fieldType = new ClassTypeDetailsImpl(
				fieldTypeClass, TypeDetails.Kind.CLASS);
		return entityClass.applyAttribute(
				fieldName, fieldType, false, false, modelsContext);
	}

	private ClassDetails resolveOrCreateClassDetails(String simpleName, String className) {
		MutableClassDetailsRegistry registry =
				(MutableClassDetailsRegistry) modelsContext.getClassDetailsRegistry();
		return registry.resolveClassDetails(
				className,
				name -> new DynamicClassDetails(
						simpleName, name, Object.class,
						false, null, null, modelsContext));
	}

	private void registerClassDetails(ClassDetails classDetails) {
		MutableClassDetailsRegistry registry =
				(MutableClassDetailsRegistry) modelsContext.getClassDetailsRegistry();
		registry.addClassDetails(classDetails);
	}
}
