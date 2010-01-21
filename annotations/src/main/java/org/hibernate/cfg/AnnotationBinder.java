// $Id$
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.cfg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.MappedSuperclass;
import javax.persistence.MapsId;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.PrimaryKeyJoinColumns;
import javax.persistence.SequenceGenerator;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Version;
import javax.persistence.ElementCollection;
import javax.persistence.CollectionTable;
import javax.persistence.UniqueConstraint;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyJoinColumns;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.OrderColumn;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.JoinColumnsOrFormulas;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.OrderBy;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Parent;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.Tuplizer;
import org.hibernate.annotations.Tuplizers;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.hibernate.annotations.Where;
import org.hibernate.annotations.GenericGenerators;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMethod;
import org.hibernate.annotations.common.reflection.XPackage;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.cfg.annotations.CollectionBinder;
import org.hibernate.cfg.annotations.EntityBinder;
import org.hibernate.cfg.annotations.Nullability;
import org.hibernate.cfg.annotations.PropertyBinder;
import org.hibernate.cfg.annotations.QueryBinder;
import org.hibernate.cfg.annotations.SimpleValueBinder;
import org.hibernate.cfg.annotations.TableBinder;
import org.hibernate.cfg.annotations.MapKeyColumnDelegator;
import org.hibernate.cfg.annotations.MapKeyJoinColumnDelegator;
import org.hibernate.engine.FilterDefinition;
import org.hibernate.engine.Versioning;
import org.hibernate.id.MultipleHiLoPerTableGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.SequenceHiLoGenerator;
import org.hibernate.id.TableHiLoGenerator;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.IdGenerator;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.persister.entity.JoinedSubclassEntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.type.TypeFactory;
import org.hibernate.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSR 175 annotation binder
 * Will read the annotation from classes, apply the
 * principles of the EJB3 spec and produces the Hibernate
 * configuration-time metamodel (the classes in the <tt>mapping</tt>
 * package)
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@SuppressWarnings("unchecked")
public final class AnnotationBinder {

	/*
	 * Some design description
	 * I tried to remove any link to annotation except from the 2 first level of
	 * method call.
	 * It'll enable to:
	 *   - facilitate annotation overriding
	 *   - mutualize one day xml and annotation binder (probably a dream though)
	 *   - split this huge class in smaller mapping oriented classes
	 *
	 * bindSomething usually create the mapping container and is accessed by one of the 2 first level method
	 * makeSomething usually create the mapping container and is accessed by bindSomething[else]
	 * fillSomething take the container into parameter and fill it.
	 */
	private AnnotationBinder() {
	}

	private static final Logger log = LoggerFactory.getLogger( AnnotationBinder.class );

	public static void bindDefaults(ExtendedMappings mappings) {
		Map defaults = mappings.getReflectionManager().getDefaults();
		{
			List<SequenceGenerator> anns = (List<SequenceGenerator>) defaults.get( SequenceGenerator.class );
			if ( anns != null ) {
				for (SequenceGenerator ann : anns) {
					IdGenerator idGen = buildIdGenerator( ann, mappings );
					if ( idGen != null ) mappings.addDefaultGenerator( idGen );
				}
			}
		}
		{
			List<TableGenerator> anns = (List<TableGenerator>) defaults.get( TableGenerator.class );
			if ( anns != null ) {
				for (TableGenerator ann : anns) {
					IdGenerator idGen = buildIdGenerator( ann, mappings );
					if ( idGen != null ) mappings.addDefaultGenerator( idGen );
				}
			}
		}
		{
			List<NamedQuery> anns = (List<NamedQuery>) defaults.get( NamedQuery.class );
			if ( anns != null ) {
				for (NamedQuery ann : anns) {
					QueryBinder.bindQuery( ann, mappings, true );
				}
			}
		}
		{
			List<NamedNativeQuery> anns = (List<NamedNativeQuery>) defaults.get( NamedNativeQuery.class );
			if ( anns != null ) {
				for (NamedNativeQuery ann : anns) {
					QueryBinder.bindNativeQuery( ann, mappings, true );
				}
			}
		}
		{
			List<SqlResultSetMapping> anns = (List<SqlResultSetMapping>) defaults.get( SqlResultSetMapping.class );
			if ( anns != null ) {
				for (SqlResultSetMapping ann : anns) {
					QueryBinder.bindSqlResultsetMapping( ann, mappings, true );
				}
			}
		}
	}

	public static void bindPackage(String packageName, ExtendedMappings mappings) {
		XPackage pckg;
		try {
			pckg = mappings.getReflectionManager().packageForName( packageName );
		}
		catch (ClassNotFoundException cnf) {
			log.warn( "Package not found or wo package-info.java: {}", packageName );
			return;
		}
		if ( pckg.isAnnotationPresent( SequenceGenerator.class ) ) {
			SequenceGenerator ann = pckg.getAnnotation( SequenceGenerator.class );
			IdGenerator idGen = buildIdGenerator( ann, mappings );
			mappings.addGenerator( idGen );
			log.trace( "Add sequence generator with name: {}", idGen.getName() );
		}
		if ( pckg.isAnnotationPresent( TableGenerator.class ) ) {
			TableGenerator ann = pckg.getAnnotation( TableGenerator.class );
			IdGenerator idGen = buildIdGenerator( ann, mappings );
			mappings.addGenerator( idGen );

		}
		bindGenericGenerators(pckg, mappings);
		bindQueries( pckg, mappings );
		bindFilterDefs( pckg, mappings );
		bindTypeDefs( pckg, mappings );
		BinderHelper.bindAnyMetaDefs( pckg, mappings );
	}

	private static void bindGenericGenerators(XAnnotatedElement annotatedElement, ExtendedMappings mappings) {
		GenericGenerator defAnn = annotatedElement.getAnnotation( GenericGenerator.class );
		GenericGenerators defsAnn = annotatedElement.getAnnotation( GenericGenerators.class );
		if ( defAnn != null ) {
			bindGenericGenerator( defAnn, mappings );
		}
		if ( defsAnn != null ) {
			for (GenericGenerator def : defsAnn.value() ) {
				bindGenericGenerator( def, mappings );
			}
		}
	}

	private static void bindGenericGenerator(GenericGenerator def, ExtendedMappings mappings) {
		IdGenerator idGen = buildIdGenerator( def, mappings );
		mappings.addGenerator( idGen );
	}

	private static void bindQueries(XAnnotatedElement annotatedElement, ExtendedMappings mappings) {
		{
			SqlResultSetMapping ann = annotatedElement.getAnnotation( SqlResultSetMapping.class );
			QueryBinder.bindSqlResultsetMapping( ann, mappings, false );
		}
		{
			SqlResultSetMappings ann = annotatedElement.getAnnotation( SqlResultSetMappings.class );
			if ( ann != null ) {
				for (SqlResultSetMapping current : ann.value()) {
					QueryBinder.bindSqlResultsetMapping( current, mappings, false );
				}
			}
		}
		{
			NamedQuery ann = annotatedElement.getAnnotation( NamedQuery.class );
			QueryBinder.bindQuery( ann, mappings, false );
		}
		{
			org.hibernate.annotations.NamedQuery ann = annotatedElement.getAnnotation(
					org.hibernate.annotations.NamedQuery.class
			);
			QueryBinder.bindQuery( ann, mappings );
		}
		{
			NamedQueries ann = annotatedElement.getAnnotation( NamedQueries.class );
			QueryBinder.bindQueries( ann, mappings, false );
		}
		{
			org.hibernate.annotations.NamedQueries ann = annotatedElement.getAnnotation(
					org.hibernate.annotations.NamedQueries.class
			);
			QueryBinder.bindQueries( ann, mappings );
		}
		{
			NamedNativeQuery ann = annotatedElement.getAnnotation( NamedNativeQuery.class );
			QueryBinder.bindNativeQuery( ann, mappings, false );
		}
		{
			org.hibernate.annotations.NamedNativeQuery ann = annotatedElement.getAnnotation(
					org.hibernate.annotations.NamedNativeQuery.class
			);
			QueryBinder.bindNativeQuery( ann, mappings );
		}
		{
			NamedNativeQueries ann = annotatedElement.getAnnotation( NamedNativeQueries.class );
			QueryBinder.bindNativeQueries( ann, mappings, false );
		}
		{
			org.hibernate.annotations.NamedNativeQueries ann = annotatedElement.getAnnotation(
					org.hibernate.annotations.NamedNativeQueries.class
			);
			QueryBinder.bindNativeQueries( ann, mappings );
		}
	}

	private static IdGenerator buildIdGenerator(java.lang.annotation.Annotation ann, Mappings mappings) {
		IdGenerator idGen = new IdGenerator();
		if ( mappings.getSchemaName() != null ) {
			idGen.addParam( PersistentIdentifierGenerator.SCHEMA, mappings.getSchemaName() );
		}
		if ( mappings.getCatalogName() != null ) {
			idGen.addParam( PersistentIdentifierGenerator.CATALOG, mappings.getCatalogName() );
		}
		if ( ann == null ) {
			idGen = null;
		}
		else if ( ann instanceof TableGenerator ) {
			TableGenerator tabGen = (TableGenerator) ann;
			idGen.setName( tabGen.name() );
			idGen.setIdentifierGeneratorStrategy( MultipleHiLoPerTableGenerator.class.getName() );

			if ( !BinderHelper.isDefault( tabGen.table() ) ) {
				idGen.addParam( MultipleHiLoPerTableGenerator.ID_TABLE, tabGen.table() );
			}
			if ( !BinderHelper.isDefault( tabGen.catalog() ) ) {
				idGen.addParam( MultipleHiLoPerTableGenerator.CATALOG, tabGen.catalog() );
			}
			if ( !BinderHelper.isDefault( tabGen.schema() ) ) {
				idGen.addParam( MultipleHiLoPerTableGenerator.SCHEMA, tabGen.schema() );
			}
			//FIXME implements uniqueconstrains

			if ( !BinderHelper.isDefault( tabGen.pkColumnName() ) ) {
				idGen.addParam( MultipleHiLoPerTableGenerator.PK_COLUMN_NAME, tabGen.pkColumnName() );
			}
			if ( !BinderHelper.isDefault( tabGen.valueColumnName() ) ) {
				idGen.addParam( MultipleHiLoPerTableGenerator.VALUE_COLUMN_NAME, tabGen.valueColumnName() );
			}
			if ( !BinderHelper.isDefault( tabGen.pkColumnValue() ) ) {
				idGen.addParam( MultipleHiLoPerTableGenerator.PK_VALUE_NAME, tabGen.pkColumnValue() );
			}
			idGen.addParam( TableHiLoGenerator.MAX_LO, String.valueOf( tabGen.allocationSize() - 1 ) );
			log.trace( "Add table generator with name: {}", idGen.getName() );
		}
		else if ( ann instanceof SequenceGenerator ) {
			SequenceGenerator seqGen = (SequenceGenerator) ann;
			idGen.setName( seqGen.name() );
			idGen.setIdentifierGeneratorStrategy( "seqhilo" );

			if ( !BinderHelper.isDefault( seqGen.sequenceName() ) ) {
				idGen.addParam( org.hibernate.id.SequenceGenerator.SEQUENCE, seqGen.sequenceName() );
			}
			//FIXME: work on initialValue() through SequenceGenerator.PARAMETERS
			//		steve : or just use o.h.id.enhanced.SequenceStyleGenerator
			if ( seqGen.initialValue() != 1 ) {
				log.warn(
						"Hibernate does not support SequenceGenerator.initialValue()"
				);
			}
			idGen.addParam( SequenceHiLoGenerator.MAX_LO, String.valueOf( seqGen.allocationSize() - 1 ) );
			log.trace( "Add sequence generator with name: {}", idGen.getName() );
		}
		else if ( ann instanceof GenericGenerator ) {
			GenericGenerator genGen = (GenericGenerator) ann;
			idGen.setName( genGen.name() );
			idGen.setIdentifierGeneratorStrategy( genGen.strategy() );
			Parameter[] params = genGen.parameters();
			for (Parameter parameter : params) {
				idGen.addParam( parameter.name(), parameter.value() );
			}
			log.trace( "Add generic generator with name: {}", idGen.getName() );
		}
		else {
			throw new AssertionFailure( "Unknown Generator annotation: " + ann );
		}
		return idGen;
	}

	/**
	 * Bind a class having JSR175 annotations
	 * The subclasses <b>have to</b> be binded after its mother class
	 *
	 * @param clazzToProcess entity to bind as {@code XClass} instance
	 * @param inheritanceStatePerClass Meta data about the inheritance relationships for all mapped classes
	 * @param mappings Mapping meta data
	 * @throws MappingException in case there is an configuration error
	 */
	public static void bindClass(
			XClass clazzToProcess, Map<XClass, InheritanceState> inheritanceStatePerClass, ExtendedMappings mappings
	) throws MappingException {
		//TODO: be more strict with secondarytable allowance (not for ids, not for secondary table join columns etc)
		InheritanceState inheritanceState = inheritanceStatePerClass.get( clazzToProcess );
		AnnotatedClassType classType = mappings.getClassType( clazzToProcess );

		//Queries declared in MappedSuperclass should be usable in Subclasses
		if ( AnnotatedClassType.EMBEDDABLE_SUPERCLASS.equals( classType ) ) {
			bindQueries( clazzToProcess, mappings );
			bindTypeDefs(clazzToProcess, mappings);
			bindFilterDefs(clazzToProcess, mappings);
		}

		if( !isEntityClassType( clazzToProcess, classType ) ) {
			return;
		}

		log.info( "Binding entity from annotated class: {}", clazzToProcess.getName() );

		PersistentClass superEntity = getSuperEntity(clazzToProcess, inheritanceStatePerClass, mappings, inheritanceState);

		bindQueries( clazzToProcess, mappings );
		bindFilterDefs( clazzToProcess, mappings );
		bindTypeDefs( clazzToProcess, mappings );
		BinderHelper.bindAnyMetaDefs( clazzToProcess, mappings );

		String schema = "";
		String table = ""; //might be no @Table annotation on the annotated class
		String catalog = "";
		List<UniqueConstraintHolder> uniqueConstraints = new ArrayList<UniqueConstraintHolder>();
		if ( clazzToProcess.isAnnotationPresent( javax.persistence.Table.class ) ) {
			javax.persistence.Table tabAnn = clazzToProcess.getAnnotation( javax.persistence.Table.class );
			table = tabAnn.name();
			schema = tabAnn.schema();
			catalog = tabAnn.catalog();
			uniqueConstraints = TableBinder.buildUniqueConstraintHolders( tabAnn.uniqueConstraints() );
		}

		Ejb3JoinColumn[] inheritanceJoinedColumns = makeInheritanceJoinColumns( clazzToProcess, mappings, inheritanceState, superEntity );
		Ejb3DiscriminatorColumn discriminatorColumn = null;
		String discrimValue = null;
		if ( InheritanceType.SINGLE_TABLE.equals( inheritanceState.getType() ) ) {
			javax.persistence.DiscriminatorColumn discAnn = clazzToProcess.getAnnotation(
					javax.persistence.DiscriminatorColumn.class
			);
			DiscriminatorType discriminatorType = discAnn != null ?
					discAnn.discriminatorType() :
					DiscriminatorType.STRING;

			org.hibernate.annotations.DiscriminatorFormula discFormulaAnn = clazzToProcess.getAnnotation(
					org.hibernate.annotations.DiscriminatorFormula.class
			);
			if ( !inheritanceState.hasParents() ) {
				discriminatorColumn = Ejb3DiscriminatorColumn.buildDiscriminatorColumn(
						discriminatorType, discAnn, discFormulaAnn, mappings
				);
			}
			if ( discAnn != null && inheritanceState.hasParents() ) {
				log.warn(
						"Discriminator column has to be defined in the root entity, it will be ignored in subclass: {}",
						clazzToProcess.getName()
				);
			}

			discrimValue = clazzToProcess.isAnnotationPresent( DiscriminatorValue.class ) ?
					clazzToProcess.getAnnotation( DiscriminatorValue.class ).value() :
					null;
		}

		PersistentClass persistentClass = makePersistentClass( inheritanceState, superEntity );

		Proxy proxyAnn = clazzToProcess.getAnnotation( Proxy.class );
		BatchSize sizeAnn = clazzToProcess.getAnnotation( BatchSize.class );
		Where whereAnn = clazzToProcess.getAnnotation( Where.class );
		Entity entityAnn = clazzToProcess.getAnnotation( Entity.class );
		org.hibernate.annotations.Entity hibEntityAnn = clazzToProcess.getAnnotation(
				org.hibernate.annotations.Entity.class
		);
		org.hibernate.annotations.Cache cacheAnn = clazzToProcess.getAnnotation(
				org.hibernate.annotations.Cache.class
		);
		EntityBinder entityBinder = new EntityBinder(
				entityAnn, hibEntityAnn, clazzToProcess, persistentClass, mappings
		);
		entityBinder.setDiscriminatorValue( discrimValue );
		entityBinder.setBatchSize( sizeAnn );
		entityBinder.setProxy( proxyAnn );
		entityBinder.setWhere( whereAnn );
		entityBinder.setCache( cacheAnn );
		entityBinder.setInheritanceState( inheritanceState );

		//Filters are not allowed on subclasses
		if ( !inheritanceState.hasParents() ) {
			bindFilters(clazzToProcess, entityBinder, mappings);
		}

		entityBinder.bindEntity();

		if ( inheritanceState.hasTable() ) {
			Check checkAnn = clazzToProcess.getAnnotation( Check.class );
			String constraints = checkAnn == null ?
					null :
					checkAnn.constraints();
			entityBinder.bindTable(
					schema, catalog, table, uniqueConstraints,
					constraints, inheritanceState.hasDenormalizedTable() ?
					superEntity.getTable() :
					null
			);
		}
		else {
			if ( clazzToProcess.isAnnotationPresent( Table.class ) ) {
				log.warn( "Illegal use of @Table in a subclass of a SINGLE_TABLE hierarchy: " + clazzToProcess
						.getName() );
			}
		}

		PropertyHolder propertyHolder = PropertyHolderBuilder.buildPropertyHolder(
				clazzToProcess,
				persistentClass,
				entityBinder, mappings, inheritanceStatePerClass
		);

		javax.persistence.SecondaryTable secTabAnn = clazzToProcess.getAnnotation(
				javax.persistence.SecondaryTable.class
		);
		javax.persistence.SecondaryTables secTabsAnn = clazzToProcess.getAnnotation(
				javax.persistence.SecondaryTables.class
		);
		entityBinder.firstLevelSecondaryTablesBinding( secTabAnn, secTabsAnn );

		OnDelete onDeleteAnn = clazzToProcess.getAnnotation( OnDelete.class );
		boolean onDeleteAppropriate = false;
		if ( InheritanceType.JOINED.equals( inheritanceState.getType() ) && inheritanceState.hasParents() ) {
			onDeleteAppropriate = true;
			final JoinedSubclass jsc = (JoinedSubclass) persistentClass;
			if ( persistentClass.getEntityPersisterClass() == null ) {
				persistentClass.getRootClass().setEntityPersisterClass( JoinedSubclassEntityPersister.class );
			}
			SimpleValue key = new DependantValue( jsc.getTable(), jsc.getIdentifier() );
			jsc.setKey( key );
			ForeignKey fk = clazzToProcess.getAnnotation( ForeignKey.class );
			if ( fk != null && !BinderHelper.isDefault( fk.name() ) ) {
				key.setForeignKeyName( fk.name() );
			}
			if ( onDeleteAnn != null ) {
				key.setCascadeDeleteEnabled( OnDeleteAction.CASCADE.equals( onDeleteAnn.action() ) );
			}
			else {
				key.setCascadeDeleteEnabled( false );
			}
			//we are never in a second pass at that stage, so queue it
			SecondPass sp = new JoinedSubclassFkSecondPass( jsc, inheritanceJoinedColumns, key, mappings );
			mappings.addSecondPass( sp );
			mappings.addSecondPass( new CreateKeySecondPass( jsc ) );

		}
		else if ( InheritanceType.SINGLE_TABLE.equals( inheritanceState.getType() ) ) {
			if ( inheritanceState.hasParents() ) {
				if ( persistentClass.getEntityPersisterClass() == null ) {
					persistentClass.getRootClass().setEntityPersisterClass( SingleTableEntityPersister.class );
				}
			}
			else {
				if ( inheritanceState.hasSiblings() || !discriminatorColumn.isImplicit() ) {
					//need a discriminator column
					bindDiscriminatorToPersistentClass(
							(RootClass) persistentClass,
							discriminatorColumn,
							entityBinder.getSecondaryTables(),
							propertyHolder
					);
					entityBinder.bindDiscriminatorValue();//bind it again since the type might have changed
				}
			}
		}
		else if ( InheritanceType.TABLE_PER_CLASS.equals( inheritanceState.getType() ) ) {
			if ( inheritanceState.hasParents() ) {
				if ( persistentClass.getEntityPersisterClass() == null ) {
					persistentClass.getRootClass().setEntityPersisterClass( UnionSubclassEntityPersister.class );
				}
			}
		}
		if ( onDeleteAnn != null && !onDeleteAppropriate ) {
			log.warn(
					"Inapropriate use of @OnDelete on entity, annotation ignored: {}", propertyHolder.getEntityName()
			);
		}

		// try to find class level generators
		HashMap<String, IdGenerator> classGenerators = buildLocalGenerators( clazzToProcess, mappings );

		// check properties
		List<PropertyData> elements =
				getElementsToProcess(
						persistentClass, clazzToProcess, inheritanceStatePerClass, entityBinder, mappings
				);
		final boolean subclassAndSingleTableStrategy = inheritanceState.getType() == InheritanceType.SINGLE_TABLE
				&& inheritanceState.hasParents();
		//process idclass if any
		Set<String> idProperties = new HashSet<String>();
		IdClass idClass = null;
		XClass current = null;
		if ( !inheritanceState.hasParents() ) {
			//look for idClass
			InheritanceState state = inheritanceState;
			do {
				current = state.getClazz();
				if ( current.isAnnotationPresent( IdClass.class ) ) {
					idClass = current.getAnnotation( IdClass.class );
					break;
				}
				state = InheritanceState.getSuperclassInheritanceState( current, inheritanceStatePerClass );
			}
			while ( state != null );
		}
		if ( idClass != null ) {
			XClass compositeClass = mappings.getReflectionManager().toXClass( idClass.value() );
			boolean isComponent = true;
			AccessType propertyAccessor = entityBinder.getPropertyAccessor( compositeClass );
			String generatorType = "assigned";
			String generator = BinderHelper.ANNOTATION_STRING_DEFAULT;
			PropertyData inferredData = new PropertyPreloadedData(
					entityBinder.getPropertyAccessType(), "id", compositeClass
			);
			PropertyData baseInferredData = new PropertyPreloadedData(
                  entityBinder.getPropertyAccessType(), "id", current
            );
			HashMap<String, IdGenerator> localGenerators = new HashMap<String, IdGenerator>();
			boolean ignoreIdAnnotations = entityBinder.isIgnoreIdAnnotations();
			entityBinder.setIgnoreIdAnnotations( true );
			bindId(
					generatorType,
					generator,
					inferredData,
					baseInferredData,
					null,
					propertyHolder,
					localGenerators,
					isComponent,
					propertyAccessor, entityBinder,
					true,
					false, mappings, inheritanceStatePerClass
			);
			inferredData = new PropertyPreloadedData(
					propertyAccessor, "_identifierMapper", compositeClass
			);
			Component mapper = fillComponent(
					propertyHolder,
					inferredData,
					baseInferredData,
					propertyAccessor, false,
					entityBinder,
					true, true,
					false, mappings, inheritanceStatePerClass
			);
			entityBinder.setIgnoreIdAnnotations( ignoreIdAnnotations );
			persistentClass.setIdentifierMapper( mapper );

			//If id definition is on a mapped superclass, update the mapping
			final org.hibernate.mapping.MappedSuperclass superclass = BinderHelper.getMappedSuperclassOrNull(
					inferredData.getDeclaringClass(),
					inheritanceStatePerClass,
					mappings
			);
			if (superclass != null) {
				superclass.setDeclaredIdentifierMapper(mapper);
			}
			else {
				//we are for sure on the entity
				persistentClass.setDeclaredIdentifierMapper( mapper );
			}

			Property property = new Property();
			property.setName( "_identifierMapper" );
			property.setNodeName( "id" );
			property.setUpdateable( false );
			property.setInsertable( false );
			property.setValue( mapper );
			property.setPropertyAccessorName( "embedded" );
			persistentClass.addProperty( property );
			entityBinder.setIgnoreIdAnnotations( true );

			Iterator properties = mapper.getPropertyIterator();
			while ( properties.hasNext() ) {
				idProperties.add( ( (Property) properties.next() ).getName() );
			}
		}
		Set<String> missingIdProperties = new HashSet<String>( idProperties );
		for (PropertyData propertyAnnotatedElement : elements) {
			String propertyName = propertyAnnotatedElement.getPropertyName();
			if ( !idProperties.contains( propertyName ) ) {
				processElementAnnotations(
						propertyHolder,
						subclassAndSingleTableStrategy ?
								Nullability.FORCED_NULL :
								Nullability.NO_CONSTRAINT,
						propertyAnnotatedElement.getProperty(),
						propertyAnnotatedElement, classGenerators, entityBinder,
						false, false, false, mappings, inheritanceStatePerClass
				);
			}
			else {
				missingIdProperties.remove( propertyName );
			}
		}

		if ( missingIdProperties.size() != 0 ) {
			StringBuilder missings = new StringBuilder();
			for (String property : missingIdProperties) {
				missings.append( property ).append( ", " );
			}
			throw new AnnotationException(
					"Unable to find properties ("
							+ missings.substring( 0, missings.length() - 2 )
							+ ") in entity annotated with @IdClass:" + persistentClass.getEntityName()
			);
		}

		if ( !inheritanceState.hasParents() ) {
			final RootClass rootClass = (RootClass) persistentClass;
			mappings.addSecondPass( new CreateKeySecondPass( rootClass ) );
		}
		else {
			superEntity.addSubclass( (Subclass) persistentClass );
		}

		mappings.addClass( persistentClass );

		//Process secondary tables and complementary definitions (ie o.h.a.Table)
		mappings.addSecondPass( new SecondaryTableSecondPass( entityBinder, propertyHolder, clazzToProcess ) );

		//add process complementary Table definition (index & all)
		entityBinder.processComplementaryTableDefinitions( clazzToProcess.getAnnotation( org.hibernate.annotations.Table.class ) );
		entityBinder.processComplementaryTableDefinitions( clazzToProcess.getAnnotation( org.hibernate.annotations.Tables.class ) );

	}

	private static PersistentClass makePersistentClass(InheritanceState inheritanceState, PersistentClass superEntity) {
		//we now know what kind of persistent entity it is
		PersistentClass persistentClass;
		//create persistent class
		if ( !inheritanceState.hasParents() ) {
			persistentClass = new RootClass();
		}
		else if ( InheritanceType.SINGLE_TABLE.equals( inheritanceState.getType() ) ) {
			persistentClass = new SingleTableSubclass( superEntity );
		}
		else if ( InheritanceType.JOINED.equals( inheritanceState.getType() ) ) {
			persistentClass = new JoinedSubclass( superEntity );
		}
		else if ( InheritanceType.TABLE_PER_CLASS.equals( inheritanceState.getType() ) ) {
			persistentClass = new UnionSubclass( superEntity );
		}
		else {
			throw new AssertionFailure( "Unknown inheritance type: " + inheritanceState.getType() );
		}
		return persistentClass;
	}

	private static Ejb3JoinColumn[] makeInheritanceJoinColumns(XClass clazzToProcess, ExtendedMappings mappings, InheritanceState inheritanceState, PersistentClass superEntity) {
		Ejb3JoinColumn[] inheritanceJoinedColumns = null;
		final boolean hasJoinedColumns = inheritanceState.hasParents()
				&& InheritanceType.JOINED.equals( inheritanceState.getType() );
		if ( hasJoinedColumns ) {
			//@Inheritance(JOINED) subclass need to link back to the super entity
			PrimaryKeyJoinColumns jcsAnn = clazzToProcess.getAnnotation( PrimaryKeyJoinColumns.class );
			boolean explicitInheritanceJoinedColumns = jcsAnn != null && jcsAnn.value().length != 0;
			if ( explicitInheritanceJoinedColumns ) {
				int nbrOfInhJoinedColumns = jcsAnn.value().length;
				PrimaryKeyJoinColumn jcAnn;
				inheritanceJoinedColumns = new Ejb3JoinColumn[nbrOfInhJoinedColumns];
				for (int colIndex = 0; colIndex < nbrOfInhJoinedColumns; colIndex++) {
					jcAnn = jcsAnn.value()[colIndex];
					inheritanceJoinedColumns[colIndex] = Ejb3JoinColumn.buildJoinColumn(
							jcAnn, null, superEntity.getIdentifier(),
							( Map<String, Join> ) null, ( PropertyHolder ) null, mappings
					);
				}
			}
			else {
				PrimaryKeyJoinColumn jcAnn = clazzToProcess.getAnnotation( PrimaryKeyJoinColumn.class );
				inheritanceJoinedColumns = new Ejb3JoinColumn[1];
				inheritanceJoinedColumns[0] = Ejb3JoinColumn.buildJoinColumn(
						jcAnn, null, superEntity.getIdentifier(),
						(Map<String, Join>) null, (PropertyHolder) null, mappings
				);
			}
			log.trace( "Subclass joined column(s) created" );
		}
		else {
			if ( clazzToProcess.isAnnotationPresent( PrimaryKeyJoinColumns.class )
					|| clazzToProcess.isAnnotationPresent( PrimaryKeyJoinColumn.class ) ) {
				log.warn( "Root entity should not hold an PrimaryKeyJoinColum(s), will be ignored" );
			}
		}
		return inheritanceJoinedColumns;
	}

	private static PersistentClass getSuperEntity(XClass clazzToProcess, Map<XClass, InheritanceState> inheritanceStatePerClass, ExtendedMappings mappings, InheritanceState inheritanceState) {
		InheritanceState superEntityState = InheritanceState.getInheritanceStateOfSuperEntity( clazzToProcess, inheritanceStatePerClass );
		PersistentClass superEntity = superEntityState != null ?
				mappings.getClass(
						superEntityState.getClazz().getName()
				) :
				null;
		if ( superEntity == null ) {
			//check if superclass is not a potential persistent class
			if ( inheritanceState.hasParents() ) {
				throw new AssertionFailure(
						"Subclass has to be binded after it's mother class: "
								+ superEntityState.getClazz().getName()
				);
			}
		}
		return superEntity;
	}

	private static boolean isEntityClassType(XClass clazzToProcess, AnnotatedClassType classType) {
		if ( AnnotatedClassType.EMBEDDABLE_SUPERCLASS.equals( classType ) //will be processed by their subentities
				|| AnnotatedClassType.NONE.equals( classType ) //to be ignored
				|| AnnotatedClassType.EMBEDDABLE.equals( classType ) //allow embeddable element declaration
				) {
			if ( AnnotatedClassType.NONE.equals( classType )
					&& clazzToProcess.isAnnotationPresent( org.hibernate.annotations.Entity.class ) ) {
				log.warn( "Class annotated @org.hibernate.annotations.Entity but not javax.persistence.Entity "
						+ "(most likely a user error): {}", clazzToProcess.getName() );
			}
			return false;
		}

		if ( !classType.equals( AnnotatedClassType.ENTITY ) ) {
			throw new AnnotationException(
					"Annotated class should have a @javax.persistence.Entity, @javax.persistence.Embeddable or @javax.persistence.EmbeddedSuperclass annotation: " + clazzToProcess
							.getName()
			);
		}

		return true;
	}

	/*
	 * Get the annotated elements, guessing the access type from @Id or @EmbeddedId presence.
	 * Change EntityBinder by side effect
	 */
	private static List<PropertyData> getElementsToProcess(
			PersistentClass persistentClass, XClass clazzToProcess,
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			EntityBinder entityBinder, ExtendedMappings mappings
	) {
		InheritanceState inheritanceState = inheritanceStatePerClass.get( clazzToProcess );
		assert !inheritanceState.isEmbeddableSuperclass();


		List<XClass> classesToProcess = getMappedSuperclassesTillNextEntityOrdered(
				persistentClass, clazzToProcess, inheritanceStatePerClass, mappings
		);

		AccessType accessType = determineDefaultAccessType( clazzToProcess, inheritanceStatePerClass );

		List<PropertyData> elements = new ArrayList<PropertyData>();
		int deep = classesToProcess.size();
		boolean hasIdentifier = false;

		for ( int index = 0; index < deep; index++ ) {
			PropertyContainer properyContainer = new PropertyContainer( classesToProcess.get( index ), clazzToProcess );
			boolean currentHasIdentifier = addElementsOfClass( elements, accessType, properyContainer, mappings );
			hasIdentifier = hasIdentifier || currentHasIdentifier;
		}

		entityBinder.setPropertyAccessType( accessType );

		if ( !hasIdentifier && !inheritanceState.hasParents() ) {
			throw new AnnotationException( "No identifier specified for entity: " + clazzToProcess.getName() );
		}

		return elements;
	}

	private static AccessType determineDefaultAccessType(XClass annotatedClass, Map<XClass, InheritanceState> inheritanceStatePerClass) {
		XClass xclass = annotatedClass;
		while ( xclass != null && !Object.class.getName().equals( xclass.getName() ) ) {
			if ( xclass.isAnnotationPresent( Entity.class ) || xclass.isAnnotationPresent( MappedSuperclass.class ) ) {
				for ( XProperty prop : xclass.getDeclaredProperties( AccessType.PROPERTY.getType() ) ) {
					if ( prop.isAnnotationPresent( Id.class ) || prop.isAnnotationPresent( EmbeddedId.class ) ) {
						return AccessType.PROPERTY;
					}
				}
				for ( XProperty prop : xclass.getDeclaredProperties( AccessType.FIELD.getType() ) ) {
					if ( prop.isAnnotationPresent( Id.class ) || prop.isAnnotationPresent( EmbeddedId.class ) ) {
						return AccessType.FIELD;
					}
				}
			}
			xclass = xclass.getSuperclass();
		}
		throw new AnnotationException( "No identifier specified for entity: " + annotatedClass.getName() );
	}

	private static List<XClass> getMappedSuperclassesTillNextEntityOrdered(
			PersistentClass persistentClass, XClass annotatedClass,
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			ExtendedMappings mappings
	) {

		//ordered to allow proper messages on properties subclassing
		List<XClass> classesToProcess = new ArrayList<XClass>();
		XClass currentClassInHierarchy = annotatedClass;
		InheritanceState superclassState;
		final ReflectionManager reflectionManager = mappings.getReflectionManager();
		do {
			classesToProcess.add( 0, currentClassInHierarchy );
			XClass superClass = currentClassInHierarchy;
			do {
				superClass = superClass.getSuperclass();
				superclassState = inheritanceStatePerClass.get( superClass );
			}
			while ( superClass != null && !reflectionManager
					.equals( superClass, Object.class ) && superclassState == null );

			currentClassInHierarchy = superClass;
		}
		while ( superclassState != null && superclassState.isEmbeddableSuperclass() );

		//add @MappedSuperclass in the metadata
		// classes from 0 to n-1 are @MappedSuperclass and should be linked
		org.hibernate.mapping.MappedSuperclass mappedSuperclass = null;
		final InheritanceState superEntityState =
				InheritanceState.getInheritanceStateOfSuperEntity( annotatedClass, inheritanceStatePerClass );
		PersistentClass superEntity =
				superEntityState != null ?
						mappings.getClass( superEntityState.getClazz().getName() ) :
						null;
		final int lastMappedSuperclass = classesToProcess.size() - 1;
		for ( int index = 0 ; index < lastMappedSuperclass ; index++ ) {
			org.hibernate.mapping.MappedSuperclass parentSuperclass = mappedSuperclass;
			final Class<?> type = mappings.getReflectionManager().toClass( classesToProcess.get( index ) );
			//add MAppedSuperclass if not already there
			mappedSuperclass = mappings.getMappedSuperclass( type );
			if (mappedSuperclass == null) {
				mappedSuperclass = new org.hibernate.mapping.MappedSuperclass(parentSuperclass, superEntity );
				mappedSuperclass.setMappedClass( type );
				mappings.addMappedSuperclass( type, mappedSuperclass );
			}
		}
		if (mappedSuperclass != null) {
			persistentClass.setSuperMappedSuperclass(mappedSuperclass);
		}
		return classesToProcess;
	}

	/*
	 * Process the filters defined on the given class, as well as all filters defined
	 * on the MappedSuperclass(s) in the inheritance hierarchy
	 */
	private static void bindFilters(XClass annotatedClass, EntityBinder entityBinder,
			ExtendedMappings mappings) {

		bindFilters(annotatedClass, entityBinder);

		XClass classToProcess = annotatedClass.getSuperclass();
		while (classToProcess != null) {
			AnnotatedClassType classType = mappings.getClassType( classToProcess );
			if ( AnnotatedClassType.EMBEDDABLE_SUPERCLASS.equals( classType ) ) {
				bindFilters(classToProcess, entityBinder);
			}
			classToProcess = classToProcess.getSuperclass();
		}

	}

	private static void bindFilters(XAnnotatedElement annotatedElement, EntityBinder entityBinder) {

		Filters filtersAnn = annotatedElement.getAnnotation( Filters.class );
		if ( filtersAnn != null ) {
			for (Filter filter : filtersAnn.value()) {
				entityBinder.addFilter( filter.name(), filter.condition() );
			}
		}

		Filter filterAnn = annotatedElement.getAnnotation( Filter.class );
		if ( filterAnn != null ) {
			entityBinder.addFilter( filterAnn.name(), filterAnn.condition() );
		}
	}

	private static void bindFilterDefs(XAnnotatedElement annotatedElement, ExtendedMappings mappings) {
		FilterDef defAnn = annotatedElement.getAnnotation( FilterDef.class );
		FilterDefs defsAnn = annotatedElement.getAnnotation( FilterDefs.class );
		if ( defAnn != null ) {
			bindFilterDef( defAnn, mappings );
		}
		if ( defsAnn != null ) {
			for (FilterDef def : defsAnn.value()) {
				bindFilterDef( def, mappings );
			}
		}
	}

	private static void bindFilterDef(FilterDef defAnn, ExtendedMappings mappings) {
		Map<String, org.hibernate.type.Type> params = new HashMap<String, org.hibernate.type.Type>();
		for (ParamDef param : defAnn.parameters()) {
			params.put( param.name(), TypeFactory.heuristicType( param.type() ) );
		}
		FilterDefinition def = new FilterDefinition( defAnn.name(), defAnn.defaultCondition(), params );
		log.info( "Binding filter definition: {}", def.getFilterName() );
		mappings.addFilterDefinition( def );
	}

	private static void bindTypeDefs(XAnnotatedElement annotatedElement, ExtendedMappings mappings) {
		TypeDef defAnn = annotatedElement.getAnnotation( TypeDef.class );
		TypeDefs defsAnn = annotatedElement.getAnnotation( TypeDefs.class );
		if ( defAnn != null ) {
			bindTypeDef( defAnn, mappings );
		}
		if ( defsAnn != null ) {
			for (TypeDef def : defsAnn.value()) {
				bindTypeDef( def, mappings );
			}
		}
	}

	private static void bindTypeDef(TypeDef defAnn, ExtendedMappings mappings) {
		Properties params = new Properties();
		for (Parameter param : defAnn.parameters()) {
			params.setProperty( param.name(), param.value() );
		}

		if (BinderHelper.isDefault(defAnn.name()) && defAnn.defaultForType().equals(void.class)) {
			throw new AnnotationException(
					"Either name or defaultForType (or both) attribute should be set in TypeDef having typeClass " +
					defAnn.typeClass().getName());
		}

		if (!BinderHelper.isDefault(defAnn.name())) {
			log.info( "Binding type definition: {}", defAnn.name() );
			mappings.addTypeDef( defAnn.name(), defAnn.typeClass().getName(), params );
		}
		if (!defAnn.defaultForType().equals(void.class)) {
			log.info( "Binding type definition: {}", defAnn.defaultForType().getName() );
			mappings.addTypeDef( defAnn.defaultForType().getName(), defAnn.typeClass().getName(), params );
		}

	}


	private static void bindDiscriminatorToPersistentClass(
			RootClass rootClass,
			Ejb3DiscriminatorColumn discriminatorColumn, Map<String, Join> secondaryTables,
			PropertyHolder propertyHolder
	) {
		if ( rootClass.getDiscriminator() == null ) {
			if ( discriminatorColumn == null ) {
				throw new AssertionFailure( "discriminator column should have been built" );
			}
			discriminatorColumn.setJoins( secondaryTables );
			discriminatorColumn.setPropertyHolder( propertyHolder );
			SimpleValue discrim = new SimpleValue( rootClass.getTable() );
			rootClass.setDiscriminator( discrim );
			discriminatorColumn.linkWithValue( discrim );
			discrim.setTypeName( discriminatorColumn.getDiscriminatorTypeName() );
			rootClass.setPolymorphic( true );
			log.trace( "Setting discriminator for entity {}", rootClass.getEntityName() );
		}
	}

	/**
	 *
	 * @param elements List of {@code ProperyData} instances
	 * @param defaultAccessType The default value access strategy which has to be used in case no explicit local access
	 *        strategy is used
	 * @param propertyContainer Metadata about a class and its properties
	 * @param mappings Mapping meta data
	 * @return {@code true} in case an id property was found while iterating the elements of {@code annoatedClass} using
	 * the determined access strategy, {@code false} otherwise.
	 */
	private static boolean addElementsOfClass(
			List<PropertyData> elements, AccessType defaultAccessType, PropertyContainer propertyContainer, ExtendedMappings mappings
	) {
		boolean hasIdentifier = false;
		AccessType accessType = defaultAccessType;

		if ( propertyContainer.hasExplicitAccessStrategy() ) {
			accessType = propertyContainer.getExplicitAccessStrategy();
		}

		propertyContainer.assertTypesAreResolvable( accessType );
		Collection<XProperty> properties = propertyContainer.getProperties( accessType );
		for ( XProperty p : properties ) {
			final boolean currentHasIdentifier = addProperty(
					propertyContainer, p, elements, accessType.getType(), mappings
			);
			hasIdentifier = hasIdentifier || currentHasIdentifier;
		}
		return hasIdentifier;
	}

	private static boolean addProperty(
			PropertyContainer propertyContainer, XProperty property, List<PropertyData> annElts,
			String propertyAccessor, ExtendedMappings mappings
	) {
		final XClass declaringClass = propertyContainer.getDeclaringClass();
		final XClass entity = propertyContainer.getEntityAtStake();
		boolean hasIdentifier;
		PropertyData propertyAnnotatedElement = new PropertyInferredData(
				declaringClass, property, propertyAccessor,
				mappings.getReflectionManager() );

		/*
		 * put element annotated by @Id in front
		 * since it has to be parsed before any association by Hibernate
		 */
		final XAnnotatedElement element = propertyAnnotatedElement.getProperty();
		if ( element.isAnnotationPresent( Id.class ) || element.isAnnotationPresent( EmbeddedId.class ) ) {
			annElts.add( 0, propertyAnnotatedElement );
			hasIdentifier = true;
		}
		else {
			annElts.add( propertyAnnotatedElement );
			hasIdentifier = false;
		}
		if ( element.isAnnotationPresent( MapsId.class  ) ) {
			mappings.addPropertyAnnotatedWithMapsId( entity, propertyAnnotatedElement );
		}

		return hasIdentifier;
	}

	/*
	 * Process annotation of a particular property
	 */
	private static void processElementAnnotations(
			PropertyHolder propertyHolder, Nullability nullability, XProperty property,
			PropertyData inferredData, HashMap<String, IdGenerator> classGenerators,
			EntityBinder entityBinder, boolean isIdentifierMapper,
			boolean isComponentEmbedded, boolean inSecondPass, ExtendedMappings mappings,
			Map<XClass, InheritanceState> inheritanceStatePerClass
	)
			throws MappingException {
		/**
		 * inSecondPass can only be used to apply right away the second pass of a composite-element
		 * Because it's a value type, there is no bidirectional association, hence second pass
		 * ordering does not matter
		 */
		Ejb3Column[] columns = null;

		log.trace(
				"Processing annotations of {}.{}", propertyHolder.getEntityName(), inferredData.getPropertyName()
		);

		if ( property.isAnnotationPresent( Parent.class ) ) {
			if ( propertyHolder.isComponent() ) {
				propertyHolder.setParentProperty( property.getName() );
			}
			else {
				throw new AnnotationException(
						"@Parent cannot be applied outside an embeddable object: "
								+ BinderHelper.getPath( propertyHolder, inferredData )
				);
			}
			return;
		}
		Ejb3JoinColumn[] joinColumns = buildExplicitJoinColumns(
				propertyHolder, property, inferredData, entityBinder, mappings
		);


		if ( property.isAnnotationPresent( Column.class ) || property.isAnnotationPresent( Formula.class ) ) {
			Column ann = property.getAnnotation( Column.class );
			Formula formulaAnn = property.getAnnotation( Formula.class );
			columns = Ejb3Column.buildColumnFromAnnotation(
					new Column[] { ann }, formulaAnn, nullability, propertyHolder, inferredData,
					entityBinder.getSecondaryTables(), mappings
			);
		}
		else if ( property.isAnnotationPresent( Columns.class ) ) {
			Columns anns = property.getAnnotation( Columns.class );
			columns = Ejb3Column.buildColumnFromAnnotation(
					anns.columns(), null, nullability, propertyHolder, inferredData, entityBinder.getSecondaryTables(),
					mappings
			);
		}

		//set default values if needed
		if ( joinColumns == null &&
				( property.isAnnotationPresent( ManyToOne.class )
						|| property.isAnnotationPresent( OneToOne.class ) )
				) {
			joinColumns = buildDefaultJoinColumnsForXToOne(
					propertyHolder, property, inferredData, entityBinder, mappings
			);
		}
		else if ( joinColumns == null &&
				( property.isAnnotationPresent( OneToMany.class )
						|| property.isAnnotationPresent( CollectionOfElements.class ) //legacy Hibernate
						|| property.isAnnotationPresent( ElementCollection.class )
				) ) {
			OneToMany oneToMany = property.getAnnotation( OneToMany.class );
			String mappedBy = oneToMany != null ?
					oneToMany.mappedBy() :
					"";
			joinColumns = Ejb3JoinColumn.buildJoinColumns(
					(JoinColumn[]) null,
					mappedBy, entityBinder.getSecondaryTables(),
					propertyHolder, inferredData.getPropertyName(), mappings
			);
		}
		else if ( joinColumns == null && property.isAnnotationPresent( org.hibernate.annotations.Any.class ) ) {
			throw new AnnotationException( "@Any requires an explicit @JoinColumn(s): "
					+ BinderHelper.getPath( propertyHolder, inferredData ) );
		}
		if ( columns == null && !property.isAnnotationPresent( ManyToMany.class ) ) {
			//useful for collection of embedded elements
			columns = Ejb3Column.buildColumnFromAnnotation(
					null, null, nullability, propertyHolder, inferredData, entityBinder.getSecondaryTables(), mappings
			);
		}

		if ( nullability == Nullability.FORCED_NOT_NULL ) {
			//force columns to not null
			for (Ejb3Column col : columns) {
				col.forceNotNull();
			}
		}

		final XClass returnedClass = inferredData.getClassOrElement();

		boolean isId = !entityBinder.isIgnoreIdAnnotations() &&
				( property.isAnnotationPresent( Id.class )
						|| property.isAnnotationPresent( EmbeddedId.class ) );
		if ( property.isAnnotationPresent( Version.class ) ) {
			if ( isIdentifierMapper ) {
				throw new AnnotationException(
						"@IdClass class should not have @Version property"
				);
			}
			if ( !( propertyHolder.getPersistentClass() instanceof RootClass ) ) {
				throw new AnnotationException(
						"Unable to define/override @Version on a subclass: "
								+ propertyHolder.getEntityName()
				);
			}
			if ( ! propertyHolder.isEntity() ) {
				throw new AnnotationException(
						"Unable to define @Version on an embedded class: "
								+ propertyHolder.getEntityName()
				);
			}
			log.trace( "{} is a version property", inferredData.getPropertyName() );
			RootClass rootClass = (RootClass) propertyHolder.getPersistentClass();
			PropertyBinder propBinder = new PropertyBinder();
			propBinder.setName( inferredData.getPropertyName() );
			propBinder.setReturnedClassName( inferredData.getTypeName() );
			propBinder.setLazy( false );
			propBinder.setAccessType( inferredData.getDefaultAccess() );
			propBinder.setColumns( columns );
			propBinder.setHolder( propertyHolder ); //PropertyHolderBuilder.buildPropertyHolder(rootClass)
			propBinder.setProperty( property );
			propBinder.setReturnedClass( inferredData.getPropertyClass() );
			propBinder.setMappings( mappings );
			propBinder.setDeclaringClass( inferredData.getDeclaringClass() );
			Property prop = propBinder.makePropertyValueAndBind();
			propBinder.getSimpleValueBinder().setVersion(true);
			rootClass.setVersion( prop );

			//If version is on a mapped superclass, update the mapping
			final org.hibernate.mapping.MappedSuperclass superclass = BinderHelper.getMappedSuperclassOrNull(
					inferredData.getDeclaringClass(),
					inheritanceStatePerClass,
					mappings
			);
			if (superclass != null) {
				superclass.setDeclaredVersion(prop);
			}
			else {
				//we know the property is on the actual entity
				rootClass.setDeclaredVersion( prop );
			}

			SimpleValue simpleValue = (SimpleValue) prop.getValue();
			simpleValue.setNullValue( "undefined" );
			rootClass.setOptimisticLockMode( Versioning.OPTIMISTIC_LOCK_VERSION );
			log.trace(
					"Version name: {}, unsavedValue: {}", rootClass.getVersion().getName(),
					( (SimpleValue) rootClass.getVersion().getValue() ).getNullValue()
			);
		}
		else if ( property.isAnnotationPresent( ManyToOne.class ) ) {
			ManyToOne ann = property.getAnnotation( ManyToOne.class );

			//check validity
			if ( property.isAnnotationPresent( Column.class )
					|| property.isAnnotationPresent( Columns.class ) ) {
				throw new AnnotationException( "@Column(s) not allowed on a @ManyToOne property: "
						+ BinderHelper.getPath( propertyHolder, inferredData ) );
			}

			Cascade hibernateCascade = property.getAnnotation( Cascade.class );
			NotFound notFound = property.getAnnotation( NotFound.class );
			boolean ignoreNotFound = notFound != null && notFound.action().equals( NotFoundAction.IGNORE );
			OnDelete onDeleteAnn = property.getAnnotation( OnDelete.class );
			boolean onDeleteCascade = onDeleteAnn != null && OnDeleteAction.CASCADE.equals( onDeleteAnn.action() );
			JoinTable assocTable = propertyHolder.getJoinTable( property );
			if ( assocTable != null ) {
				Join join = propertyHolder.addJoin( assocTable, false );
				for (Ejb3JoinColumn joinColumn : joinColumns) {
					joinColumn.setSecondaryTableName( join.getTable().getName() );
				}
			}
			final boolean mandatory = !ann.optional() || property.isAnnotationPresent( MapsId.class );
			bindManyToOne(
					getCascadeStrategy( ann.cascade(), hibernateCascade, false),
					joinColumns,
					!mandatory,
					ignoreNotFound, onDeleteCascade,
					ToOneBinder.getTargetEntity( inferredData, mappings ),
					propertyHolder,
					inferredData, false, isIdentifierMapper, inSecondPass, mappings
			);
		}
		else if ( property.isAnnotationPresent( OneToOne.class ) ) {
			OneToOne ann = property.getAnnotation( OneToOne.class );

			//check validity
			if ( property.isAnnotationPresent( Column.class )
					|| property.isAnnotationPresent( Columns.class ) ) {
				throw new AnnotationException( "@Column(s) not allowed on a @OneToOne property: "
						+ BinderHelper.getPath( propertyHolder, inferredData ) );
			}

			//FIXME support a proper PKJCs
			boolean trueOneToOne = property.isAnnotationPresent( PrimaryKeyJoinColumn.class )
					|| property.isAnnotationPresent( PrimaryKeyJoinColumns.class );
			Cascade hibernateCascade = property.getAnnotation( Cascade.class );
			NotFound notFound = property.getAnnotation( NotFound.class );
			boolean ignoreNotFound = notFound != null && notFound.action().equals( NotFoundAction.IGNORE );
			OnDelete onDeleteAnn = property.getAnnotation( OnDelete.class );
			boolean onDeleteCascade = onDeleteAnn != null && OnDeleteAction.CASCADE.equals( onDeleteAnn.action() );
			JoinTable assocTable = propertyHolder.getJoinTable( property );
			if ( assocTable != null ) {
				Join join = propertyHolder.addJoin( assocTable, false );
				for (Ejb3JoinColumn joinColumn : joinColumns) {
					joinColumn.setSecondaryTableName( join.getTable().getName() );
				}
			}
			//MapsId means the columns belong to the pk => not null
			final boolean mandatory = !ann.optional() || property.isAnnotationPresent( MapsId.class );
			bindOneToOne(
					getCascadeStrategy( ann.cascade(), hibernateCascade, ann.orphanRemoval()),
					joinColumns,
					!mandatory,
					getFetchMode( ann.fetch() ),
					ignoreNotFound, onDeleteCascade,
					ToOneBinder.getTargetEntity( inferredData, mappings ),
					propertyHolder,
					inferredData, ann.mappedBy(), trueOneToOne, isIdentifierMapper, inSecondPass, mappings
			);
		}
		else if ( property.isAnnotationPresent( org.hibernate.annotations.Any.class ) ) {

			//check validity
			if ( property.isAnnotationPresent( Column.class )
					|| property.isAnnotationPresent( Columns.class ) ) {
				throw new AnnotationException( "@Column(s) not allowed on a @Any property: "
						+ BinderHelper.getPath( propertyHolder, inferredData ) );
			}

			Cascade hibernateCascade = property.getAnnotation( Cascade.class );
			OnDelete onDeleteAnn = property.getAnnotation( OnDelete.class );
			boolean onDeleteCascade = onDeleteAnn != null && OnDeleteAction.CASCADE.equals( onDeleteAnn.action() );
			JoinTable assocTable = propertyHolder.getJoinTable( property );
			if ( assocTable != null ) {
				Join join = propertyHolder.addJoin( assocTable, false );
				for (Ejb3JoinColumn joinColumn : joinColumns) {
					joinColumn.setSecondaryTableName( join.getTable().getName() );
				}
			}
			bindAny( getCascadeStrategy( null, hibernateCascade, false), //@Any has not cascade attribute
					joinColumns, onDeleteCascade, nullability,
					propertyHolder, inferredData, entityBinder,
					isIdentifierMapper, mappings );
		}
		else if ( property.isAnnotationPresent( OneToMany.class )
				|| property.isAnnotationPresent( ManyToMany.class )
				|| property.isAnnotationPresent( CollectionOfElements.class ) //legacy Hibernate
				|| property.isAnnotationPresent( ElementCollection.class )
				|| property.isAnnotationPresent( ManyToAny.class ) ) {
			OneToMany oneToManyAnn = property.getAnnotation( OneToMany.class );
			ManyToMany manyToManyAnn = property.getAnnotation( ManyToMany.class );
			ElementCollection elementCollectionAnn = property.getAnnotation( ElementCollection.class );
			CollectionOfElements collectionOfElementsAnn = property.getAnnotation( CollectionOfElements.class ); //legacy hibernate

			final IndexColumn indexColumn;

			if ( property.isAnnotationPresent( OrderColumn.class ) ) {
				indexColumn = IndexColumn.buildColumnFromAnnotation(
						property.getAnnotation(OrderColumn.class),
						propertyHolder,
						inferredData,
						entityBinder.getSecondaryTables(),
						mappings
				);
			}
			else {
				//if @IndexColumn is not there, the generated IndexColumn is an implicit column and not used.
				//so we can leave the legacy processing as the default
				indexColumn = IndexColumn.buildColumnFromAnnotation(
						property.getAnnotation(org.hibernate.annotations.IndexColumn.class),
						propertyHolder,
						inferredData,
						mappings
				);
			}
			CollectionBinder collectionBinder = CollectionBinder.getCollectionBinder(
					propertyHolder.getEntityName(),
					property,
					!indexColumn.isImplicit(),
					property.isAnnotationPresent( CollectionOfElements.class )
					|| property.isAnnotationPresent( org.hibernate.annotations.MapKey.class )
							// || property.isAnnotationPresent( ManyToAny.class )
			);
			collectionBinder.setIndexColumn( indexColumn );
			MapKey mapKeyAnn = property.getAnnotation( MapKey.class );
			collectionBinder.setMapKey( mapKeyAnn );
			collectionBinder.setPropertyName( inferredData.getPropertyName() );
			BatchSize batchAnn = property.getAnnotation( BatchSize.class );
			collectionBinder.setBatchSize( batchAnn );
			javax.persistence.OrderBy ejb3OrderByAnn = property.getAnnotation( javax.persistence.OrderBy.class );
			OrderBy orderByAnn = property.getAnnotation( OrderBy.class );
			collectionBinder.setEjb3OrderBy( ejb3OrderByAnn );
			collectionBinder.setSqlOrderBy( orderByAnn );
			Sort sortAnn = property.getAnnotation( Sort.class );
			collectionBinder.setSort( sortAnn );
			Cache cachAnn = property.getAnnotation( Cache.class );
			collectionBinder.setCache( cachAnn );
			collectionBinder.setPropertyHolder( propertyHolder );
			Cascade hibernateCascade = property.getAnnotation( Cascade.class );
			NotFound notFound = property.getAnnotation( NotFound.class );
			boolean ignoreNotFound = notFound != null && notFound.action().equals( NotFoundAction.IGNORE );
			collectionBinder.setIgnoreNotFound( ignoreNotFound );
			collectionBinder.setCollectionType( inferredData.getProperty().getElementClass() );
			collectionBinder.setMappings( mappings );
			collectionBinder.setAccessType( inferredData.getDefaultAccess() );

			Ejb3Column[] elementColumns;
			//do not use "element" if you are a JPA 2 @ElementCollection only for legacy Hibernate mappings
			boolean isJPA2ForValueMapping = property.isAnnotationPresent( ElementCollection.class );
			PropertyData virtualProperty = isJPA2ForValueMapping ? inferredData : new WrappedInferredData( inferredData, "element" );
			if ( property.isAnnotationPresent( Column.class ) || property.isAnnotationPresent(
					Formula.class
			) ) {
				Column ann = property.getAnnotation( Column.class );
				Formula formulaAnn = property.getAnnotation( Formula.class );
				elementColumns = Ejb3Column.buildColumnFromAnnotation(
						new Column[] { ann },
						formulaAnn,
						nullability,
						propertyHolder,
						virtualProperty,
						entityBinder.getSecondaryTables(),
						mappings
				);
			}
			else if ( property.isAnnotationPresent( Columns.class ) ) {
				Columns anns = property.getAnnotation( Columns.class );
				elementColumns = Ejb3Column.buildColumnFromAnnotation(
						anns.columns(), null, nullability, propertyHolder, virtualProperty,
						entityBinder.getSecondaryTables(), mappings
				);
			}
			else {
				elementColumns = Ejb3Column.buildColumnFromAnnotation(
						null,
						null,
						nullability,
						propertyHolder,
						virtualProperty,
						entityBinder.getSecondaryTables(),
						mappings
				);
			}
			{
				Column[] keyColumns = null;
				//JPA 2 has priority and has different default column values, differenciate legacy from JPA 2
				Boolean isJPA2 = null;
				if ( property.isAnnotationPresent( MapKeyColumn.class ) ) {
					isJPA2 = Boolean.TRUE;
					keyColumns = new Column[] { new MapKeyColumnDelegator( property.getAnnotation( MapKeyColumn.class ) ) };
				}
				else if ( property.isAnnotationPresent( org.hibernate.annotations.MapKey.class ) ) {
					if ( isJPA2 == null) {
						isJPA2 = Boolean.FALSE;
					}
					keyColumns = property.getAnnotation( org.hibernate.annotations.MapKey.class ).columns();
				}

				//not explicitly legacy
				if ( isJPA2 == null) {
					isJPA2 = Boolean.TRUE;
				}

				//nullify empty array
				keyColumns = keyColumns != null && keyColumns.length > 0 ? keyColumns : null;

				//"mapkey" is the legacy column name of the key column pre JPA 2
				PropertyData mapKeyVirtualProperty = new WrappedInferredData( inferredData, "mapkey" );
				Ejb3Column[] mapColumns = Ejb3Column.buildColumnFromAnnotation(
						keyColumns,
						null,
						Nullability.FORCED_NOT_NULL,
						propertyHolder,
						isJPA2 ? inferredData : mapKeyVirtualProperty,
						isJPA2 ? "_KEY" : null,
						entityBinder.getSecondaryTables(),
						mappings
				);
				collectionBinder.setMapKeyColumns( mapColumns );
			}
			{
				JoinColumn[] joinKeyColumns = null;
				//JPA 2 has priority and has different default column values, differenciate legacy from JPA 2
				Boolean isJPA2 = null;
				if ( property.isAnnotationPresent( MapKeyJoinColumns.class ) ) {
					isJPA2 = Boolean.TRUE;
					final MapKeyJoinColumn[] mapKeyJoinColumns = property.getAnnotation( MapKeyJoinColumns.class ).value();
					joinKeyColumns = new JoinColumn[mapKeyJoinColumns.length];
					int index = 0;
					for ( MapKeyJoinColumn joinColumn : mapKeyJoinColumns ) {
						joinKeyColumns[index] = new MapKeyJoinColumnDelegator( joinColumn );
						index++;
					}
					if ( joinKeyColumns != null ) {
						throw new AnnotationException( "@MapKeyJoinColumn and @MapKeyJoinColumns used on the same property: "
								+ BinderHelper.getPath( propertyHolder, inferredData ) );
					}
				}
				else if ( property.isAnnotationPresent( MapKeyJoinColumn.class ) ) {
					isJPA2 = Boolean.TRUE;
					joinKeyColumns = new JoinColumn[] { new MapKeyJoinColumnDelegator( property.getAnnotation( MapKeyJoinColumn.class ) ) };
				}
				else if ( property.isAnnotationPresent( org.hibernate.annotations.MapKeyManyToMany.class ) ) {
					if ( isJPA2 == null) {
						isJPA2 = Boolean.FALSE;
					}
					joinKeyColumns = property.getAnnotation( org.hibernate.annotations.MapKeyManyToMany.class ).joinColumns();
				}

				//not explicitly legacy
				if ( isJPA2 == null) {
					isJPA2 = Boolean.TRUE;
				}

	            PropertyData mapKeyVirtualProperty = new WrappedInferredData( inferredData, "mapkey" );
				Ejb3JoinColumn[] mapJoinColumns = Ejb3JoinColumn.buildJoinColumnsWithDefaultColumnSuffix(
						joinKeyColumns,
						null,
						entityBinder.getSecondaryTables(),
						propertyHolder,
						isJPA2 ? inferredData.getPropertyName() : mapKeyVirtualProperty.getPropertyName(),
						isJPA2 ? "_KEY" : null,
						mappings
				);
				collectionBinder.setMapKeyManyToManyColumns( mapJoinColumns );
			}

			//potential element
			collectionBinder.setEmbedded( property.isAnnotationPresent( Embedded.class ) );
			collectionBinder.setElementColumns( elementColumns );
			collectionBinder.setProperty( property );

			//TODO enhance exception with @ManyToAny and @CollectionOfElements
			if ( oneToManyAnn != null && manyToManyAnn != null ) {
				throw new AnnotationException(
						"@OneToMany and @ManyToMany on the same property is not allowed: "
								+ propertyHolder.getEntityName() + "." + inferredData.getPropertyName()
				);
			}
			String mappedBy = null;
			if ( oneToManyAnn != null ) {
				for (Ejb3JoinColumn column : joinColumns) {
					if ( column.isSecondary() ) {
						throw new NotYetImplementedException( "Collections having FK in secondary table" );
					}
				}
				collectionBinder.setFkJoinColumns( joinColumns );
				mappedBy = oneToManyAnn.mappedBy();
				collectionBinder.setTargetEntity(
						mappings.getReflectionManager().toXClass( oneToManyAnn.targetEntity() )
				);
				collectionBinder.setCascadeStrategy(
						getCascadeStrategy( oneToManyAnn.cascade(), hibernateCascade, oneToManyAnn.orphanRemoval()) );
				collectionBinder.setOneToMany( true );
			}
			else if ( elementCollectionAnn != null
					|| collectionOfElementsAnn != null //Hibernate legacy
					) {
				for (Ejb3JoinColumn column : joinColumns) {
					if ( column.isSecondary() ) {
						throw new NotYetImplementedException( "Collections having FK in secondary table" );
					}
				}
				collectionBinder.setFkJoinColumns( joinColumns );
				mappedBy = "";
				final Class<?> targetElement = elementCollectionAnn != null ?
						elementCollectionAnn.targetClass() :
						collectionOfElementsAnn.targetElement();
				collectionBinder.setTargetEntity(
						mappings.getReflectionManager().toXClass( targetElement )
				);
				//collectionBinder.setCascadeStrategy( getCascadeStrategy( embeddedCollectionAnn.cascade(), hibernateCascade ) );
				collectionBinder.setOneToMany( true );
			}
			else if ( manyToManyAnn != null ) {
				mappedBy = manyToManyAnn.mappedBy();
				collectionBinder.setTargetEntity(
						mappings.getReflectionManager().toXClass( manyToManyAnn.targetEntity() )
				);
				collectionBinder.setCascadeStrategy( getCascadeStrategy( manyToManyAnn.cascade(), hibernateCascade, false) );
				collectionBinder.setOneToMany( false );
			}
			else if ( property.isAnnotationPresent( ManyToAny.class ) ) {
				mappedBy = "";
				collectionBinder.setTargetEntity(
						mappings.getReflectionManager().toXClass( void.class )
				);
				collectionBinder.setCascadeStrategy( getCascadeStrategy( null, hibernateCascade, false) );
				collectionBinder.setOneToMany( false );
			}
			collectionBinder.setMappedBy( mappedBy );

			bindJoinedTableAssociation(
					property, mappings, entityBinder, collectionBinder, propertyHolder, inferredData, mappedBy
			);

			OnDelete onDeleteAnn = property.getAnnotation( OnDelete.class );
			boolean onDeleteCascade = onDeleteAnn != null && OnDeleteAction.CASCADE.equals( onDeleteAnn.action() );
			collectionBinder.setCascadeDeleteEnabled( onDeleteCascade );
			if ( isIdentifierMapper ) {
				collectionBinder.setInsertable( false );
				collectionBinder.setUpdatable( false );
			}
			if ( property.isAnnotationPresent( CollectionId.class ) ) { //do not compute the generators unless necessary
				HashMap<String, IdGenerator> localGenerators = (HashMap<String, IdGenerator>) classGenerators.clone();
				localGenerators.putAll( buildLocalGenerators( property, mappings ) );
				collectionBinder.setLocalGenerators( localGenerators );

			}
			collectionBinder.setInheritanceStatePerClass( inheritanceStatePerClass );
			collectionBinder.setDeclaringClass( inferredData.getDeclaringClass() );
			collectionBinder.bind();

		}
		//Either a regular property or a basic @Id or @EmbeddedId while not ignoring id annotations
		else if ( !isId || !entityBinder.isIgnoreIdAnnotations() ) {
			//define whether the type is a component or not
			boolean isComponent;
			isComponent = property.isAnnotationPresent( Embedded.class )
					|| property.isAnnotationPresent( EmbeddedId.class )
					|| returnedClass.isAnnotationPresent( Embeddable.class );
			PropertyBinder propertyBinder;
			if ( isComponent ) {
				AccessType propertyAccessor = entityBinder.getPropertyAccessor( property );
				propertyBinder = bindComponent(
						inferredData,
						propertyHolder,
						propertyAccessor,
						entityBinder,
						isIdentifierMapper,
						mappings,
						isComponentEmbedded,
						isId,
						inheritanceStatePerClass
				);
			}
			else {
				//provide the basic property mapping
				boolean optional = true;
				boolean lazy = false;
				if ( property.isAnnotationPresent( Basic.class ) ) {
					Basic ann = property.getAnnotation( Basic.class );
					optional = ann.optional();
					lazy = ann.fetch() == FetchType.LAZY;
				}
				//implicit type will check basic types and Serializable classes
				if ( isId || ( !optional && nullability != Nullability.FORCED_NULL ) ) {
					//force columns to not null
					for (Ejb3Column col : columns) {
						col.forceNotNull();
					}
				}

				//Override from @MapsId if needed
				if ( isId || propertyHolder.isOrWithinEmbeddedId() ) {
					columns = overrideColumnFromMapsIdProperty(
							isId ? "" : property.getName(), //@MapsId("") points to the id property
							columns,
							propertyHolder,
							entityBinder,
							mappings );
				}

				propertyBinder = new PropertyBinder();
				propertyBinder.setName( inferredData.getPropertyName() );
				propertyBinder.setReturnedClassName( inferredData.getTypeName() );
				propertyBinder.setLazy( lazy );
				propertyBinder.setAccessType( inferredData.getDefaultAccess() );
				propertyBinder.setColumns( columns );
				propertyBinder.setHolder( propertyHolder );
				propertyBinder.setProperty( property );
				propertyBinder.setReturnedClass( inferredData.getPropertyClass() );
				propertyBinder.setMappings( mappings );
				if ( isIdentifierMapper ) {
					propertyBinder.setInsertable( false );
					propertyBinder.setUpdatable( false );
				}
				propertyBinder.setDeclaringClass( inferredData.getDeclaringClass() );
				propertyBinder.setId(isId);
				propertyBinder.setInheritanceStatePerClass(inheritanceStatePerClass);
				propertyBinder.makePropertyValueAndBind();
			}
			if (isId) {
				//components and regular basic types create SimpleValue objects
				final SimpleValue value = ( SimpleValue ) propertyBinder.getValue();
				processId(
						propertyHolder,
						inferredData,
						value,
						classGenerators,
						isIdentifierMapper,
						mappings
				);
			}
		}
		//init index
		//process indexes after everything: in second pass, many to one has to be done before indexes
		Index index = property.getAnnotation( Index.class );
		if ( index != null ) {
			if ( joinColumns != null ) {

				for (Ejb3Column column : joinColumns) {
					column.addIndex( index, inSecondPass );
				}
			}
			else {
				if ( columns != null ) {
					for (Ejb3Column column : columns) {
						column.addIndex( index, inSecondPass );
					}
				}
			}
		}

		NaturalId naturalIdAnn = property.getAnnotation( NaturalId.class );
		if ( naturalIdAnn != null ) {
			if ( joinColumns != null ) {
				for (Ejb3Column column : joinColumns) {
					column.addUniqueKey( "_UniqueKey", inSecondPass );
				}
			}
			else {
				for (Ejb3Column column : columns) {
					column.addUniqueKey( "_UniqueKey", inSecondPass );
				}
			}
		}
	}

	private static void processId(PropertyHolder propertyHolder, PropertyData inferredData, SimpleValue idValue, HashMap<String, IdGenerator> classGenerators, boolean isIdentifierMapper, ExtendedMappings mappings) {
		if ( isIdentifierMapper ) {
			throw new AnnotationException(
					"@IdClass class should not have @Id nor @EmbeddedId properties: "
							+ BinderHelper.getPath( propertyHolder, inferredData )
			);
		}
		XClass returnedClass = inferredData.getClassOrElement();
		XProperty property = inferredData.getProperty();
		//clone classGenerator and override with local values
		HashMap<String, IdGenerator> localGenerators = (HashMap<String, IdGenerator>) classGenerators.clone();
		localGenerators.putAll( buildLocalGenerators( property, mappings ) );

		//manage composite related metadata
		//guess if its a component and find id data access (property, field etc)
		final boolean isComponent = returnedClass.isAnnotationPresent( Embeddable.class )
				|| property.isAnnotationPresent( EmbeddedId.class );

		GeneratedValue generatedValue = property.getAnnotation( GeneratedValue.class );
		String generatorType = generatedValue != null ?
				generatorType( generatedValue.strategy() ) :
				"assigned";
		String generatorName = generatedValue != null ?
				generatedValue.generator() :
				BinderHelper.ANNOTATION_STRING_DEFAULT;
		if ( isComponent ) generatorType = "assigned"; //a component must not have any generator
		BinderHelper.makeIdGenerator( idValue, generatorType, generatorName, mappings, localGenerators );

		log.trace(
				"Bind {} on {}", ( isComponent ? "@EmbeddedId" : "@Id" ), inferredData.getPropertyName()
		);
	}

	private static Ejb3JoinColumn[] buildDefaultJoinColumnsForXToOne(PropertyHolder propertyHolder, XProperty property, PropertyData inferredData, EntityBinder entityBinder, ExtendedMappings mappings) {
		Ejb3JoinColumn[] joinColumns;
		JoinTable joinTableAnn = propertyHolder.getJoinTable( property );
		if ( joinTableAnn != null ) {
			joinColumns = Ejb3JoinColumn.buildJoinColumns(
					joinTableAnn.inverseJoinColumns(), null, entityBinder.getSecondaryTables(),
					propertyHolder, inferredData.getPropertyName(), mappings
			);
			if ( StringHelper.isEmpty( joinTableAnn.name() ) ) {
				throw new AnnotationException(
						"JoinTable.name() on a @ToOne association has to be explicit: "
								+ BinderHelper.getPath( propertyHolder, inferredData )
				);
			}
		}
		else {
			OneToOne oneToOneAnn = property.getAnnotation( OneToOne.class );
			String mappedBy = oneToOneAnn != null ?
					oneToOneAnn.mappedBy() :
					null;
			joinColumns = Ejb3JoinColumn.buildJoinColumns(
					( JoinColumn[]) null,
					mappedBy, entityBinder.getSecondaryTables(),
					propertyHolder, inferredData.getPropertyName(), mappings
			);
		}
		return joinColumns;
	}

	private static Ejb3JoinColumn[] buildExplicitJoinColumns(PropertyHolder propertyHolder, XProperty property, PropertyData inferredData, EntityBinder entityBinder, ExtendedMappings mappings) {
		//process @JoinColumn(s) before @Column(s) to handle collection of entities properly
		Ejb3JoinColumn[] joinColumns = null;
		{
			JoinColumn[] anns = null;

			if ( property.isAnnotationPresent( JoinColumn.class ) ) {
				anns = new JoinColumn[] { property.getAnnotation( JoinColumn.class ) };
			}
			else if ( property.isAnnotationPresent( JoinColumns.class ) ) {
				JoinColumns ann = property.getAnnotation( JoinColumns.class );
				anns = ann.value();
				int length = anns.length;
				if ( length == 0 ) {
					throw new AnnotationException( "Cannot bind an empty @JoinColumns" );
				}
			}
			if ( anns != null ) {
				joinColumns = Ejb3JoinColumn.buildJoinColumns(
						anns, null, entityBinder.getSecondaryTables(),
						propertyHolder, inferredData.getPropertyName(), mappings
				);
			}
			else if ( property.isAnnotationPresent( JoinColumnsOrFormulas.class ) ) {
				JoinColumnsOrFormulas ann = property.getAnnotation( JoinColumnsOrFormulas.class );
				joinColumns = Ejb3JoinColumn.buildJoinColumnsOrFormulas(
						ann, null, entityBinder.getSecondaryTables(),
						propertyHolder, inferredData.getPropertyName(), mappings
				);
			}
		}
		return joinColumns;
	}

	//TODO move that to collection binder?
	private static void bindJoinedTableAssociation(
			XProperty property, ExtendedMappings mappings, EntityBinder entityBinder,
			CollectionBinder collectionBinder, PropertyHolder propertyHolder, PropertyData inferredData,
			String mappedBy
	) {
		TableBinder associationTableBinder = new TableBinder();
		JoinColumn[] annJoins;
		JoinColumn[] annInverseJoins;
		JoinTable assocTable = propertyHolder.getJoinTable( property );
		CollectionTable collectionTable = property.getAnnotation( CollectionTable.class );

		if ( assocTable != null || collectionTable != null ) {

			final String catalog;
			final String schema;
			final String tableName;
			final UniqueConstraint[] uniqueConstraints;
			final JoinColumn[] joins;
			final JoinColumn[] inverseJoins;

			//JPA 2 has priority
			if (collectionTable != null) {
				catalog = collectionTable.catalog();
				schema = collectionTable.schema();
				tableName = collectionTable.name();
				uniqueConstraints = collectionTable.uniqueConstraints();
				joins = collectionTable.joinColumns();
				inverseJoins = null;
			}
			else {
				catalog = assocTable.catalog();
				schema = assocTable.schema();
				tableName = assocTable.name();
				uniqueConstraints = assocTable.uniqueConstraints();
				joins = assocTable.joinColumns();
				inverseJoins = assocTable.inverseJoinColumns();
			}

			collectionBinder.setExplicitAssociationTable( true );

			if ( !BinderHelper.isDefault( schema ) ) associationTableBinder.setSchema( schema );
			if ( !BinderHelper.isDefault( catalog ) ) associationTableBinder.setCatalog( catalog );
			if ( !BinderHelper.isDefault( tableName ) ) associationTableBinder.setName( tableName );
			associationTableBinder.setUniqueConstraints( uniqueConstraints );

			//set check constaint in the second pass
			annJoins = joins.length == 0 ? null : joins;
			annInverseJoins = inverseJoins == null || inverseJoins.length == 0 ? null : inverseJoins;
		}
		else {
			annJoins = null;
			annInverseJoins = null;
		}
		Ejb3JoinColumn[] joinColumns = Ejb3JoinColumn.buildJoinTableJoinColumns(
				annJoins, entityBinder.getSecondaryTables(), propertyHolder, inferredData.getPropertyName(), mappedBy,
				mappings
		);
		Ejb3JoinColumn[] inverseJoinColumns = Ejb3JoinColumn.buildJoinTableJoinColumns(
				annInverseJoins, entityBinder.getSecondaryTables(), propertyHolder, inferredData.getPropertyName(),
				mappedBy, mappings
		);
		associationTableBinder.setMappings( mappings );
		collectionBinder.setTableBinder( associationTableBinder );
		collectionBinder.setJoinColumns( joinColumns );
		collectionBinder.setInverseJoinColumns( inverseJoinColumns );
	}

	private static PropertyBinder bindComponent(
			PropertyData inferredData,
			PropertyHolder propertyHolder,
			AccessType propertyAccessor,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			ExtendedMappings mappings,
			boolean isComponentEmbedded,
			boolean isId,
			Map<XClass, InheritanceState> inheritanceStatePerClass
	) {
		Component comp = fillComponent(
				propertyHolder, inferredData, propertyAccessor, !isId, entityBinder,
				isComponentEmbedded, isIdentifierMapper,
				false, mappings, inheritanceStatePerClass
		);
		if (isId) {
			comp.setKey( true );
			if ( propertyHolder.getPersistentClass().getIdentifier() != null ) {
				throw new AnnotationException(
						comp.getComponentClassName()
						+ " must not have @Id properties when used as an @EmbeddedId: "
						+ BinderHelper.getPath( propertyHolder, inferredData ) );
			}
			if ( comp.getPropertySpan() == 0 ) {
				throw new AnnotationException( comp.getComponentClassName()
						+ " has no persistent id property"
						+ BinderHelper.getPath( propertyHolder, inferredData ) );
			}
		}
		XProperty property = inferredData.getProperty();
		setupComponentTuplizer( property, comp );
		PropertyBinder binder = new PropertyBinder();
		binder.setName( inferredData.getPropertyName() );
		binder.setValue( comp );
		binder.setProperty( inferredData.getProperty() );
		binder.setAccessType( inferredData.getDefaultAccess() );
		binder.setEmbedded( isComponentEmbedded );
		binder.setHolder( propertyHolder );
		binder.setId( isId );
		binder.setInheritanceStatePerClass( inheritanceStatePerClass );
		binder.setMappings( mappings );
		binder.makePropertyAndBind();
		return binder;
	}

	public static Component fillComponent(
			PropertyHolder propertyHolder, PropertyData inferredData,
		    AccessType propertyAccessor, boolean isNullable,
			EntityBinder entityBinder,
			boolean isComponentEmbedded, boolean isIdentifierMapper, boolean inSecondPass,
			ExtendedMappings mappings, Map<XClass, InheritanceState> inheritanceStatePerClass
	) {

	   return fillComponent(propertyHolder, inferredData, null, propertyAccessor,
			   isNullable, entityBinder, isComponentEmbedded, isIdentifierMapper, inSecondPass, mappings,
			   inheritanceStatePerClass);
	}

	public static Component fillComponent(
          PropertyHolder propertyHolder, PropertyData inferredData, PropertyData baseInferredData,
		  AccessType propertyAccessor, boolean isNullable, EntityBinder entityBinder,
          boolean isComponentEmbedded, boolean isIdentifierMapper, boolean inSecondPass, ExtendedMappings mappings,
		  Map<XClass, InheritanceState> inheritanceStatePerClass
  ) {

		/**
		 * inSecondPass can only be used to apply right away the second pass of a composite-element
		 * Because it's a value type, there is no bidirectional association, hence second pass
		 * ordering does not matter
		 */
		Component comp = new Component( propertyHolder.getPersistentClass() );
		comp.setEmbedded( isComponentEmbedded );
		//yuk
		comp.setTable( propertyHolder.getTable() );
		if ( !isIdentifierMapper ) {
			comp.setComponentClassName( inferredData.getClassOrElementName() );
		}
		else {
			comp.setComponentClassName( comp.getOwner().getClassName() );
		}
		comp.setNodeName( inferredData.getPropertyName() );
		String subpath = BinderHelper.getPath( propertyHolder, inferredData );
		log.trace( "Binding component with path: {}", subpath );
		PropertyHolder subHolder = PropertyHolderBuilder.buildPropertyHolder(
				comp, subpath,
				inferredData, propertyHolder, mappings
		);

		final XClass entityXClass = inferredData.getPropertyClass();
		List<PropertyData> classElements = new ArrayList<PropertyData>();
		XClass returnedClassOrElement = inferredData.getClassOrElement();

		List<PropertyData> baseClassElements = null;
		XClass baseReturnedClassOrElement;
		if(baseInferredData != null)
		{
		   baseClassElements = new ArrayList<PropertyData>();
		   baseReturnedClassOrElement = baseInferredData.getClassOrElement();
		   bindTypeDefs(baseReturnedClassOrElement, mappings);
	       PropertyContainer propContainer = new PropertyContainer( baseReturnedClassOrElement, entityXClass );
		   addElementsOfClass( baseClassElements, propertyAccessor, propContainer, mappings );
		}

		//embeddable elements can have type defs
		bindTypeDefs(returnedClassOrElement, mappings);
		PropertyContainer propContainer = new PropertyContainer( returnedClassOrElement, entityXClass );
		addElementsOfClass( classElements, propertyAccessor, propContainer, mappings);

		//add elements of the embeddable superclass
		XClass superClass = entityXClass.getSuperclass();
		while ( superClass != null && superClass.isAnnotationPresent( MappedSuperclass.class ) ) {
			//FIXME: proper support of typevariables incl var resolved at upper levels
			propContainer = new PropertyContainer( superClass, entityXClass );
			addElementsOfClass( classElements, propertyAccessor, propContainer, mappings );
			superClass = superClass.getSuperclass();
		}
		if ( baseClassElements != null ) {
			if ( !hasIdClassAnnotations( entityXClass ) ) {
				for ( int i = 0; i < classElements.size(); i++ ) {
					classElements.set( i, baseClassElements.get( i ) );  //this works since they are in the same order
				}
			}
		}
		for (PropertyData propertyAnnotatedElement : classElements) {
			processElementAnnotations(
					subHolder, isNullable ?
					Nullability.NO_CONSTRAINT :
					Nullability.FORCED_NOT_NULL,
					propertyAnnotatedElement.getProperty(), propertyAnnotatedElement,
					new HashMap<String, IdGenerator>(), entityBinder, isIdentifierMapper, isComponentEmbedded,
					inSecondPass, mappings, inheritanceStatePerClass
			);
		}
		return comp;
	}

	private static void bindId(
			String generatorType, String generatorName,
			PropertyData inferredData, Ejb3Column[] columns, PropertyHolder propertyHolder,
			Map<String, IdGenerator> localGenerators,
			boolean isComposite,
			AccessType propertyAccessor, EntityBinder entityBinder, boolean isEmbedded,
			boolean isIdentifierMapper, ExtendedMappings mappings,
			Map<XClass, InheritanceState> inheritanceStatePerClass
	) {

	   bindId(generatorType, generatorName, inferredData, null, columns, propertyHolder,
			   localGenerators, isComposite, propertyAccessor, entityBinder,
			   isEmbedded, isIdentifierMapper, mappings, inheritanceStatePerClass);
	}

    private static void bindId(
          String generatorType, String generatorName, PropertyData inferredData,
          PropertyData baseInferredData, Ejb3Column[] columns, PropertyHolder propertyHolder,
          Map<String, IdGenerator> localGenerators,
          boolean isComposite,
          AccessType propertyAccessor, EntityBinder entityBinder, boolean isEmbedded,
          boolean isIdentifierMapper, ExtendedMappings mappings,
		  Map<XClass, InheritanceState> inheritanceStatePerClass
  ) {

		/*
		 * Fill simple value and property since and Id is a property
		 */
		PersistentClass persistentClass = propertyHolder.getPersistentClass();
		if ( !( persistentClass instanceof RootClass ) ) {
			throw new AnnotationException(
					"Unable to define/override @Id(s) on a subclass: "
							+ propertyHolder.getEntityName()
			);
		}
		RootClass rootClass = (RootClass) persistentClass;
		String persistentClassName = rootClass.getClassName();
		SimpleValue id;
		final String propertyName = inferredData.getPropertyName();
		if ( isComposite ) {
			id = fillComponent(
					propertyHolder, inferredData, baseInferredData, propertyAccessor,
					false, entityBinder, isEmbedded, isIdentifierMapper, false, mappings, inheritanceStatePerClass
			);
			Component componentId = (Component) id;
			componentId.setKey( true );
			if ( rootClass.getIdentifier() != null ) {
				throw new AnnotationException( componentId.getComponentClassName() + " must not have @Id properties when used as an @EmbeddedId" );
			}
			if ( componentId.getPropertySpan() == 0 ) {
				throw new AnnotationException( componentId.getComponentClassName() + " has no persistent id property" );
			}
			//tuplizers
			XProperty property = inferredData.getProperty();
			setupComponentTuplizer( property, componentId );
		}
		else {
			for (Ejb3Column column : columns) {
				column.forceNotNull(); //this is an id
			}
			SimpleValueBinder value = new SimpleValueBinder();
			value.setPropertyName( propertyName );
			value.setReturnedClassName( inferredData.getTypeName() );
			value.setColumns( columns );
			value.setPersistentClassName( persistentClassName );
			value.setMappings( mappings );
			value.setType( inferredData.getProperty(), inferredData.getClassOrElement() );
			id = value.make();
		}
		rootClass.setIdentifier( id );
		BinderHelper.makeIdGenerator( id, generatorType, generatorName, mappings, localGenerators );
		if ( isEmbedded ) {
			rootClass.setEmbeddedIdentifier( inferredData.getPropertyClass() == null );
		}
		else {
			PropertyBinder binder = new PropertyBinder();
			binder.setName( propertyName );
			binder.setValue( id );
			binder.setAccessType( inferredData.getDefaultAccess() );
			binder.setProperty( inferredData.getProperty() );
			Property prop = binder.makeProperty();
			rootClass.setIdentifierProperty( prop );
			//if the id property is on a superclass, update the metamodel
			final org.hibernate.mapping.MappedSuperclass superclass = BinderHelper.getMappedSuperclassOrNull(
					inferredData.getDeclaringClass(),
					inheritanceStatePerClass,
					mappings
			);
			if (superclass != null) {
				superclass.setDeclaredIdentifierProperty(prop);
			}
			else {
				//we know the property is on the actual entity
				rootClass.setDeclaredIdentifierProperty( prop );
			}
		}
	}

	private static Ejb3Column[] overrideColumnFromMapsIdProperty(String propertyPath,
																 Ejb3Column[] columns,
																 PropertyHolder propertyHolder,
																 EntityBinder entityBinder,
																 ExtendedMappings mappings) {
		Ejb3Column[] result = columns;
		final XClass persistentXClass;
		try {
			 persistentXClass = mappings.getReflectionManager()
					.classForName( propertyHolder.getPersistentClass().getClassName(), AnnotationBinder.class );
		}
		catch ( ClassNotFoundException e ) {
			throw new AssertionFailure( "PersistentClass name cannot be converted into a Class", e);
		}
		final PropertyData annotatedWithMapsId = mappings.getPropertyAnnotatedWithMapsId( persistentXClass, propertyPath );
		if ( annotatedWithMapsId != null ) {
			result = buildExplicitJoinColumns( propertyHolder, annotatedWithMapsId.getProperty(), annotatedWithMapsId, entityBinder, mappings );
			if (result == null) {
				result = buildDefaultJoinColumnsForXToOne( propertyHolder, annotatedWithMapsId.getProperty(), annotatedWithMapsId, entityBinder, mappings );
				throw new UnsupportedOperationException( "Implicit @JoinColumn is not supported on @MapsId properties: "
						+ annotatedWithMapsId.getDeclaringClass() + " " + annotatedWithMapsId.getPropertyName() );
			}
		}
		return result;
	}

	private static void setupComponentTuplizer(XProperty property, Component component) {
		if ( property == null ) return;
		if ( property.isAnnotationPresent( Tuplizers.class ) ) {
			for (Tuplizer tuplizer : property.getAnnotation( Tuplizers.class ).value()) {
				EntityMode mode = EntityMode.parse( tuplizer.entityMode() );
				component.addTuplizer( mode, tuplizer.impl().getName() );
			}
		}
		if ( property.isAnnotationPresent( Tuplizer.class ) ) {
			Tuplizer tuplizer = property.getAnnotation( Tuplizer.class );
			EntityMode mode = EntityMode.parse( tuplizer.entityMode() );
			component.addTuplizer( mode, tuplizer.impl().getName() );
		}
	}

	private static void bindManyToOne(
			String cascadeStrategy, Ejb3JoinColumn[] columns, boolean optional,
			boolean ignoreNotFound, boolean cascadeOnDelete,
			XClass targetEntity, PropertyHolder propertyHolder,
			PropertyData inferredData, boolean unique, boolean isIdentifierMapper, boolean inSecondPass,
			ExtendedMappings mappings
	) {
		//All FK columns should be in the same table
		org.hibernate.mapping.ManyToOne value = new org.hibernate.mapping.ManyToOne( columns[0].getTable() );
		// This is a @OneToOne mapped to a physical o.h.mapping.ManyToOne
		if ( unique ) {
			value.markAsLogicalOneToOne();
		}
		value.setReferencedEntityName( ToOneBinder.getReferenceEntityName(inferredData, targetEntity, mappings) );
		final XProperty property = inferredData.getProperty();
		defineFetchingStrategy( value, property );
		//value.setFetchMode( fetchMode );
		value.setIgnoreNotFound( ignoreNotFound );
		value.setCascadeDeleteEnabled( cascadeOnDelete );
		//value.setLazy( fetchMode != FetchMode.JOIN );
		if ( !optional ) {
			for (Ejb3JoinColumn column : columns) {
				column.setNullable( false );
			}
		}
		if ( property.isAnnotationPresent( MapsId.class ) ) {
			//read only
			for (Ejb3JoinColumn column : columns) {
				column.setInsertable( false );
				column.setUpdatable( false );
			}
		}
		value.setTypeName( inferredData.getClassOrElementName() );
		final String propertyName = inferredData.getPropertyName();
		value.setTypeUsingReflection( propertyHolder.getClassName(), propertyName );

		ForeignKey fk = property.getAnnotation( ForeignKey.class );
		String fkName = fk != null ?
				fk.name() :
				"";
		if ( !BinderHelper.isDefault( fkName ) ) value.setForeignKeyName( fkName );

		String path = propertyHolder.getPath() + "." + propertyName;
		FkSecondPass secondPass = new ToOneFkSecondPass(
				value, columns,
				!optional && unique, //cannot have nullable and unique on certain DBs like Derby
				propertyHolder.getEntityOwnerClassName(),
				path, mappings
		);
		if ( inSecondPass ) {
			secondPass.doSecondPass( mappings.getClasses() );
		}
		else {
			mappings.addSecondPass(
					secondPass
			);
		}
		Ejb3Column.checkPropertyConsistency( columns, propertyHolder.getEntityName() + propertyName );
		PropertyBinder binder = new PropertyBinder();
		binder.setName( propertyName );
		binder.setValue( value );
		//binder.setCascade(cascadeStrategy);
		if ( isIdentifierMapper ) {
			binder.setInsertable( false );
			binder.setUpdatable( false );
		}
		else {
			binder.setInsertable( columns[0].isInsertable() );
			binder.setUpdatable( columns[0].isUpdatable() );
		}
		binder.setAccessType( inferredData.getDefaultAccess() );
		binder.setCascade( cascadeStrategy );
		binder.setProperty( property );
		Property prop = binder.makeProperty();
		//composite FK columns are in the same table so its OK
		propertyHolder.addProperty( prop, columns, inferredData.getDeclaringClass() );
	}

	protected static void defineFetchingStrategy(ToOne toOne, XProperty property) {
		LazyToOne lazy = property.getAnnotation( LazyToOne.class );
		Fetch fetch = property.getAnnotation( Fetch.class );
		ManyToOne manyToOne = property.getAnnotation( ManyToOne.class );
		OneToOne oneToOne = property.getAnnotation( OneToOne.class );
		FetchType fetchType;
		if ( manyToOne != null ) {
			fetchType = manyToOne.fetch();
		}
		else if ( oneToOne != null ) {
			fetchType = oneToOne.fetch();
		}
		else {
			throw new AssertionFailure(
					"Define fetch strategy on a property not annotated with @OneToMany nor @OneToOne"
			);
		}
		if ( lazy != null ) {
			toOne.setLazy( !( lazy.value() == LazyToOneOption.FALSE ) );
			toOne.setUnwrapProxy( ( lazy.value() == LazyToOneOption.NO_PROXY ) );
		}
		else {
			toOne.setLazy( fetchType == FetchType.LAZY );
			toOne.setUnwrapProxy( false );
		}
		if ( fetch != null ) {
			if ( fetch.value() == org.hibernate.annotations.FetchMode.JOIN ) {
				toOne.setFetchMode( FetchMode.JOIN );
				toOne.setLazy( false );
				toOne.setUnwrapProxy( false );
			}
			else if ( fetch.value() == org.hibernate.annotations.FetchMode.SELECT ) {
				toOne.setFetchMode( FetchMode.SELECT );
			}
			else if ( fetch.value() == org.hibernate.annotations.FetchMode.SUBSELECT ) {
				throw new AnnotationException( "Use of FetchMode.SUBSELECT not allowed on ToOne associations" );
			}
			else {
				throw new AssertionFailure( "Unknown FetchMode: " + fetch.value() );
			}
		}
		else {
			toOne.setFetchMode( getFetchMode( fetchType ) );
		}
	}

	private static void bindOneToOne(
			String cascadeStrategy,
			Ejb3JoinColumn[] joinColumns,
			boolean optional,
			FetchMode fetchMode,
			boolean ignoreNotFound,
			boolean cascadeOnDelete,
			XClass targetEntity,
			PropertyHolder propertyHolder,
			PropertyData inferredData, String mappedBy,
			boolean trueOneToOne,
			boolean isIdentifierMapper, boolean inSecondPass, ExtendedMappings mappings
	) {
		//column.getTable() => persistentClass.getTable()
		final String propertyName = inferredData.getPropertyName();
		log.trace( "Fetching {} with {}", propertyName, fetchMode );
		boolean mapToPK = true;
		if ( !trueOneToOne ) {
			//try to find a hidden true one to one (FK == PK columns)
			KeyValue identifier = propertyHolder.getIdentifier();
			if ( identifier == null ) {
				//this is a @OneToOne in a @EmbeddedId (the persistentClass.identifier is not set yet, it's being built)
				//by definition the PK cannot refers to itself so it cannot map to itself
				mapToPK = false;
			}
			else {
				Iterator idColumns = identifier.getColumnIterator();
				List<String> idColumnNames = new ArrayList<String>();
				org.hibernate.mapping.Column currentColumn;
				if ( identifier.getColumnSpan() !=  joinColumns.length ) {
					mapToPK = false;
				}
				else {
					while ( idColumns.hasNext() ) {
						currentColumn = (org.hibernate.mapping.Column) idColumns.next();
						idColumnNames.add( currentColumn.getName() );
					}
					for (Ejb3JoinColumn col : joinColumns) {
						if ( !idColumnNames.contains( col.getMappingColumn().getName() ) ) {
							mapToPK = false;
							break;
						}
					}
				}
			}
		}
		if ( trueOneToOne || mapToPK || !BinderHelper.isDefault( mappedBy ) ) {
			//is a true one-to-one
			//FIXME referencedColumnName ignored => ordering may fail.
			OneToOneSecondPass secondPass = new OneToOneSecondPass(
					mappedBy,
					propertyHolder.getEntityName(),
					propertyName,
					propertyHolder, inferredData, targetEntity, ignoreNotFound, cascadeOnDelete,
					optional, cascadeStrategy, joinColumns, mappings
			);
			if ( inSecondPass ) {
				secondPass.doSecondPass( mappings.getClasses() );
			}
			else {
				mappings.addSecondPass(
						secondPass, BinderHelper.isDefault( mappedBy )
				);
			}
		}
		else {
			//has a FK on the table
			bindManyToOne(
					cascadeStrategy, joinColumns, optional, ignoreNotFound, cascadeOnDelete,
					targetEntity,
					propertyHolder, inferredData, true, isIdentifierMapper, inSecondPass, mappings
			);
		}
	}

	private static void bindAny(
			String cascadeStrategy, Ejb3JoinColumn[] columns, boolean cascadeOnDelete, Nullability nullability,
			PropertyHolder propertyHolder, PropertyData inferredData, EntityBinder entityBinder,
			boolean isIdentifierMapper, ExtendedMappings mappings
	) {
		org.hibernate.annotations.Any anyAnn = inferredData.getProperty().getAnnotation( org.hibernate.annotations.Any.class );
		if ( anyAnn == null ) {
			throw new AssertionFailure( "Missing @Any annotation: "
					+ BinderHelper.getPath( propertyHolder, inferredData ) );
		}
		Any value = BinderHelper.buildAnyValue( anyAnn.metaDef(), columns, anyAnn.metaColumn(), inferredData,
				cascadeOnDelete, nullability, propertyHolder, entityBinder, anyAnn.optional(), mappings );

		PropertyBinder binder = new PropertyBinder();
		binder.setName( inferredData.getPropertyName() );
		binder.setValue( value );

		binder.setLazy( anyAnn.fetch() == FetchType.LAZY );
		//binder.setCascade(cascadeStrategy);
		if ( isIdentifierMapper ) {
			binder.setInsertable( false );
			binder.setUpdatable( false );
		}
		else {
			binder.setInsertable( columns[0].isInsertable() );
			binder.setUpdatable( columns[0].isUpdatable() );
		}
		binder.setAccessType( inferredData.getDefaultAccess() );
		binder.setCascade( cascadeStrategy );
		Property prop = binder.makeProperty();
		//composite FK columns are in the same table so its OK
		propertyHolder.addProperty( prop, columns, inferredData.getDeclaringClass() );
	}

	private static String generatorType(GenerationType generatorEnum) {
		switch ( generatorEnum ) {
			case IDENTITY:
				return "identity";
			case AUTO:
				return "native";
			case TABLE:
				return MultipleHiLoPerTableGenerator.class.getName();
			case SEQUENCE:
				return "seqhilo";
		}
		throw new AssertionFailure( "Unknown GeneratorType: " + generatorEnum );
	}

	private static EnumSet<CascadeType> convertToHibernateCascadeType(javax.persistence.CascadeType[] ejbCascades) {
		EnumSet<CascadeType> hibernateCascadeSet = EnumSet.noneOf( CascadeType.class );
		if ( ejbCascades != null && ejbCascades.length > 0 ) {
			for (javax.persistence.CascadeType cascade : ejbCascades) {
				switch ( cascade ) {
					case ALL:
						hibernateCascadeSet.add( CascadeType.ALL );
						break;
					case PERSIST:
						hibernateCascadeSet.add( CascadeType.PERSIST );
						break;
					case MERGE:
						hibernateCascadeSet.add( CascadeType.MERGE );
						break;
					case REMOVE:
						hibernateCascadeSet.add( CascadeType.REMOVE );
						break;
					case REFRESH:
						hibernateCascadeSet.add( CascadeType.REFRESH );
						break;
					case DETACH:
						hibernateCascadeSet.add( CascadeType.DETACH );
						break;
				}
			}
		}

		return hibernateCascadeSet;
	}

	private static String getCascadeStrategy(
		javax.persistence.CascadeType[] ejbCascades, Cascade hibernateCascadeAnnotation,
		boolean orphanRemoval) {
		EnumSet<CascadeType> hibernateCascadeSet = convertToHibernateCascadeType( ejbCascades );
		CascadeType[] hibernateCascades = hibernateCascadeAnnotation == null ?
				null :
				hibernateCascadeAnnotation.value();

		if ( hibernateCascades != null && hibernateCascades.length > 0 ) {
			hibernateCascadeSet.addAll( Arrays.asList( hibernateCascades ) );
		}

		if ( orphanRemoval ) {
			hibernateCascadeSet.add(CascadeType.DELETE_ORPHAN);
			hibernateCascadeSet.add(CascadeType.REMOVE);
		}

		StringBuilder cascade = new StringBuilder();
		for ( CascadeType aHibernateCascadeSet : hibernateCascadeSet ) {
			switch ( aHibernateCascadeSet ) {
				case ALL:
					cascade.append( "," ).append( "all" );
					break;
				case SAVE_UPDATE:
					cascade.append( "," ).append( "save-update" );
					break;
				case PERSIST:
					cascade.append( "," ).append( "persist" );
					break;
				case MERGE:
					cascade.append( "," ).append( "merge" );
					break;
				case LOCK:
					cascade.append( "," ).append( "lock" );
					break;
				case REFRESH:
					cascade.append( "," ).append( "refresh" );
					break;
				case REPLICATE:
					cascade.append( "," ).append( "replicate" );
					break;
				case EVICT:
				case DETACH:
					cascade.append( "," ).append( "evict" );
					break;
				case DELETE:
					cascade.append( "," ).append( "delete" );
					break;
				case DELETE_ORPHAN:
					cascade.append( "," ).append( "delete-orphan" );
					break;
				case REMOVE:
					cascade.append( "," ).append( "delete" );
					break;
			}
		}
		return cascade.length() > 0 ?
				cascade.substring( 1 ) :
				"none";
	}

	public static FetchMode getFetchMode(FetchType fetch) {
		if ( fetch == FetchType.EAGER ) {
			return FetchMode.JOIN;
		}
		else {
			return FetchMode.SELECT;
		}
	}

	private static HashMap<String, IdGenerator> buildLocalGenerators(XAnnotatedElement annElt, Mappings mappings) {
		HashMap<String, IdGenerator> generators = new HashMap<String, IdGenerator>();
		TableGenerator tabGen = annElt.getAnnotation( TableGenerator.class );
		SequenceGenerator seqGen = annElt.getAnnotation( SequenceGenerator.class );
		GenericGenerator genGen = annElt.getAnnotation( GenericGenerator.class );
		if ( tabGen != null ) {
			IdGenerator idGen = buildIdGenerator( tabGen, mappings );
			generators.put( idGen.getName(), idGen );
		}
		if ( seqGen != null ) {
			IdGenerator idGen = buildIdGenerator( seqGen, mappings );
			generators.put( idGen.getName(), idGen );
		}
		if ( genGen != null ) {
			IdGenerator idGen = buildIdGenerator( genGen, mappings );
			generators.put( idGen.getName(), idGen );
		}
		return generators;
	}

	public static boolean isDefault(XClass clazz, ExtendedMappings mappings) {
		return mappings.getReflectionManager().equals( clazz, void.class );
	}

	/**
	 * For the mapped entities build some temporary data-structure containing information about the
	 * inheritance status of a class.
	 *
	 * @param orderedClasses Order list of all annotated entities and their mapped superclasses
	 * @return A map of {@code InheritanceState}s keyed against their {@code XClass}.
	 */
	public static Map<XClass, InheritanceState> buildInheritanceStates(List<XClass> orderedClasses) {
		Map<XClass, InheritanceState> inheritanceStatePerClass = new HashMap<XClass, InheritanceState>(
				orderedClasses.size()
		);
		for (XClass clazz : orderedClasses) {
			InheritanceState superclassState = InheritanceState.getSuperclassInheritanceState(
					clazz, inheritanceStatePerClass );
			InheritanceState state = new InheritanceState( clazz );
			if ( superclassState != null ) {
				//the classes are ordered thus preventing an NPE
				//FIXME if an entity has subclasses annotated @MappedSperclass wo sub @Entity this is wrong
				superclassState.setHasSiblings( true );
				InheritanceState superEntityState = InheritanceState.getInheritanceStateOfSuperEntity(
						clazz, inheritanceStatePerClass );
				state.setHasParents( superEntityState != null );
				final boolean nonDefault = state.getType() != null && !InheritanceType.SINGLE_TABLE.equals( state.getType() );
				if ( superclassState.getType() != null ) {
					final boolean mixingStrategy = state.getType() != null && !state.getType().equals( superclassState.getType() );
					if ( nonDefault && mixingStrategy ) {
						log.warn(
								"Mixing inheritance strategy in a entity hierarchy is not allowed, ignoring sub strategy in: {}",
								clazz.getName()
						);
					}
					state.setType( superclassState.getType() );
				}
			}
			inheritanceStatePerClass.put( clazz, state );
		}
		return inheritanceStatePerClass;
	}

	private static boolean hasIdClassAnnotations(XClass idClass)
	{
		if(idClass.getAnnotation(Embeddable.class) != null)
			return true;

		List<XProperty> properties = idClass.getDeclaredProperties( XClass.ACCESS_FIELD );
		for ( XProperty property : properties ) {
			if ( property.isAnnotationPresent( Column.class ) || property.isAnnotationPresent( OneToMany.class ) ||
					property.isAnnotationPresent( ManyToOne.class ) || property.isAnnotationPresent( Id.class ) ||
					property.isAnnotationPresent( GeneratedValue.class ) || property.isAnnotationPresent( OneToOne.class ) ||
					property.isAnnotationPresent( ManyToMany.class )
					) {
				return true;
			}
		}
		List<XMethod> methods = idClass.getDeclaredMethods();
		for ( XMethod method : methods ) {
			if ( method.isAnnotationPresent( Column.class ) || method.isAnnotationPresent( OneToMany.class ) ||
					method.isAnnotationPresent( ManyToOne.class ) || method.isAnnotationPresent( Id.class ) ||
					method.isAnnotationPresent( GeneratedValue.class ) || method.isAnnotationPresent( OneToOne.class ) ||
					method.isAnnotationPresent( ManyToMany.class )
					) {
				return true;
			}
		}
		return false;
	}
}
