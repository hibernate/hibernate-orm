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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.hibernate.AnnotationException;
import org.hibernate.DuplicateMappingException;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.common.reflection.MetadataProvider;
import org.hibernate.annotations.common.reflection.MetadataProviderInjector;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.cfg.annotations.Version;
import org.hibernate.cfg.annotations.reflection.JPAMetadataProvider;
import org.hibernate.cfg.beanvalidation.BeanValidationActivator;
import org.hibernate.engine.NamedQueryDefinition;
import org.hibernate.engine.NamedSQLQueryDefinition;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.event.EventListeners;
import org.hibernate.event.PreInsertEventListener;
import org.hibernate.event.PreUpdateEventListener;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.IdGenerator;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.util.JoinedIterator;
import org.hibernate.util.ReflectHelper;
import org.hibernate.util.StringHelper;
import org.hibernate.util.CollectionHelper;

/**
 * Similar to the {@link Configuration} object but handles EJB3 and Hibernate
 * specific annotations as a metadata facility.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class AnnotationConfiguration extends Configuration {
	private Logger log = LoggerFactory.getLogger( AnnotationConfiguration.class );

	/**
	 * Class name of the class needed to enable Search.
	 */
	private static final String SEARCH_STARTUP_CLASS = "org.hibernate.search.event.EventListenerRegister";

	/**
	 * Method to call to enable Search.
	 */
	private static final String SEARCH_STARTUP_METHOD = "enableHibernateSearch";

	static {
		Version.touch(); //touch version
	}

	public static final String ARTEFACT = "hibernate.mapping.precedence";
	public static final String DEFAULT_PRECEDENCE = "hbm, class";

	private Map<String, IdGenerator> namedGenerators;
	private Map<String, Map<String, Join>> joins;
	private Map<String, AnnotatedClassType> classTypes;
	private Set<String> defaultNamedQueryNames;
	private Set<String> defaultNamedNativeQueryNames;
	private Set<String> defaultSqlResulSetMappingNames;
	private Set<String> defaultNamedGenerators;
	private Map<String, Properties> generatorTables;
	private Map<Table, List<UniqueConstraintHolder>> uniqueConstraintHoldersByTable;
	private Map<String, String> mappedByResolver;
	private Map<String, String> propertyRefResolver;
	private Map<String, AnyMetaDef> anyMetaDefs;
	private List<XClass> annotatedClasses;
	private Map<String, XClass> annotatedClassEntities;
	private Map<String, Document> hbmEntities;
	private List<CacheHolder> caches;
	private List<Document> hbmDocuments; //user ordering matters, hence the list
	private String precedence = null;
	private boolean inSecondPass = false;
	private transient ReflectionManager reflectionManager;
	private boolean isDefaultProcessed = false;
	private boolean isValidatorNotPresentLogged;

	public AnnotationConfiguration() {
		super();
	}

	public AnnotationConfiguration(SettingsFactory sf) {
		super( sf );
	}

	/**
	 * Takes the list of entities annotated with {@code @Entity} or {@code @MappedSuperclass} and returns them in an
	 * ordered list.
	 *
	 * @param original The list of all entities annotated with {@code @Entity} or {@code @MappedSuperclass}
	 * @return Ordered list of entities including superclasses for entities which have any. Class hierachies are
	 * listed bottom up (starting from the top level base class). There is no indication in the list when a new class
	 * (hierarchy) starts.
	 */
	protected List<XClass> orderAndFillHierarchy(List<XClass> original) {
		List<XClass> copy = new ArrayList<XClass>( original );
		insertMappedSuperclasses( original, copy );

		// order the hierarchy
		List<XClass> workingCopy = new ArrayList<XClass>( copy );
		List<XClass> newList = new ArrayList<XClass>( copy.size() );
		while ( workingCopy.size() > 0 ) {
			XClass clazz = workingCopy.get( 0 );
			orderHierarchy( workingCopy, newList, copy, clazz );
		}
		return newList;
	}

	private void insertMappedSuperclasses(List<XClass> original, List<XClass> copy) {
		for ( XClass clazz : original ) {
			XClass superClass = clazz.getSuperclass();
			while ( superClass != null && !reflectionManager.equals( superClass, Object.class ) && !copy.contains(
					superClass
			) ) {
				if ( superClass.isAnnotationPresent( Entity.class )
						|| superClass.isAnnotationPresent( MappedSuperclass.class ) ) {
					copy.add( superClass );
				}
				superClass = superClass.getSuperclass();
			}
		}
	}

	private void orderHierarchy(List<XClass> copy, List<XClass> newList, List<XClass> original, XClass clazz) {
		if ( clazz == null || reflectionManager.equals( clazz, Object.class ) ) {
			return;
		}
		//process superclass first
		orderHierarchy( copy, newList, original, clazz.getSuperclass() );
		if ( original.contains( clazz ) ) {
			if ( !newList.contains( clazz ) ) {
				newList.add( clazz );
			}
			copy.remove( clazz );
		}
	}

	/**
	 * Read a mapping from the class annotation metadata (JSR 175).
	 *
	 * @param persistentClass the mapped class
	 *
	 * @return the configuration object
	 */
	public AnnotationConfiguration addAnnotatedClass(Class persistentClass) throws MappingException {
		XClass persistentXClass = reflectionManager.toXClass( persistentClass );
		try {
			annotatedClasses.add( persistentXClass );
			return this;
		}
		catch ( MappingException me ) {
			log.error( "Could not compile the mapping annotations", me );
			throw me;
		}
	}

	/**
	 * Read package level metadata
	 *
	 * @param packageName java package name
	 *
	 * @return the configuration object
	 */
	public AnnotationConfiguration addPackage(String packageName) throws MappingException {
		log.info( "Mapping package {}", packageName );
		try {
			AnnotationBinder.bindPackage( packageName, createExtendedMappings() );
			return this;
		}
		catch ( MappingException me ) {
			log.error( "Could not compile the mapping annotations", me );
			throw me;
		}
	}

	public ExtendedMappings createExtendedMappings() {
		return new ExtendedMappingsImpl();
	}

	@Override
	public void setCacheConcurrencyStrategy(
			String clazz, String concurrencyStrategy, String region, boolean cacheLazyProperty
	) throws MappingException {
		caches.add( new CacheHolder( clazz, concurrencyStrategy, region, true, cacheLazyProperty ) );
	}

	@Override
	public void setCollectionCacheConcurrencyStrategy(String collectionRole, String concurrencyStrategy, String region)
			throws MappingException {
		caches.add( new CacheHolder( collectionRole, concurrencyStrategy, region, false, false ) );
	}

	@Override
	protected void reset() {
		super.reset();
		namedGenerators = new HashMap();
		joins = new HashMap<String, Map<String, Join>>();
		classTypes = new HashMap<String, AnnotatedClassType>();
		generatorTables = new HashMap<String, Properties>();
		defaultNamedQueryNames = new HashSet<String>();
		defaultNamedNativeQueryNames = new HashSet<String>();
		defaultSqlResulSetMappingNames = new HashSet<String>();
		defaultNamedGenerators = new HashSet<String>();
		uniqueConstraintHoldersByTable = new HashMap<Table, List<UniqueConstraintHolder>>();
		mappedByResolver = new HashMap<String, String>();
		propertyRefResolver = new HashMap<String, String>();
		annotatedClasses = new ArrayList<XClass>();
		caches = new ArrayList<CacheHolder>();
		hbmEntities = new HashMap<String, Document>();
		annotatedClassEntities = new HashMap<String, XClass>();
		hbmDocuments = new ArrayList<Document>();
		namingStrategy = EJB3NamingStrategy.INSTANCE;
		setEntityResolver( new EJB3DTDEntityResolver() );
		anyMetaDefs = new HashMap<String, AnyMetaDef>();
		reflectionManager = new JavaReflectionManager();
		( ( MetadataProviderInjector ) reflectionManager ).setMetadataProvider( new JPAMetadataProvider() );

	}

	@Override
	protected void secondPassCompile() throws MappingException {
		log.debug( "Execute first pass mapping processing" );
		//build annotatedClassEntities
		{
			List<XClass> tempAnnotatedClasses = new ArrayList<XClass>( annotatedClasses.size() );
			for ( XClass clazz : annotatedClasses ) {
				if ( clazz.isAnnotationPresent( Entity.class ) ) {
					annotatedClassEntities.put( clazz.getName(), clazz );
					tempAnnotatedClasses.add( clazz );
				}
				else if ( clazz.isAnnotationPresent( MappedSuperclass.class ) ) {
					tempAnnotatedClasses.add( clazz );
				}
				//only keep MappedSuperclasses and Entity in this list
			}
			annotatedClasses = tempAnnotatedClasses;
		}

		//process default values first
		if ( !isDefaultProcessed ) {
			//use global delimiters if orm.xml declare it
			final Object isDelimited = reflectionManager.getDefaults().get( "delimited-identifier" );
			if (isDelimited != null && isDelimited == Boolean.TRUE) {
				getProperties().put( Environment.GLOBALLY_QUOTED_IDENTIFIERS, "true" );
			}

			AnnotationBinder.bindDefaults( createExtendedMappings() );
			isDefaultProcessed = true;
		}

		//process entities
		if ( precedence == null ) {
			precedence = getProperties().getProperty( ARTEFACT );
		}
		if ( precedence == null ) {
			precedence = DEFAULT_PRECEDENCE;
		}
		StringTokenizer precedences = new StringTokenizer( precedence, ",; ", false );
		if ( !precedences.hasMoreElements() ) {
			throw new MappingException( ARTEFACT + " cannot be empty: " + precedence );
		}
		while ( precedences.hasMoreElements() ) {
			String artifact = ( String ) precedences.nextElement();
			removeConflictedArtifact( artifact );
			processArtifactsOfType( artifact );
		}

		int cacheNbr = caches.size();
		for ( int index = 0; index < cacheNbr; index++ ) {
			CacheHolder cacheHolder = caches.get( index );
			if ( cacheHolder.isClass ) {
				super.setCacheConcurrencyStrategy(
						cacheHolder.role, cacheHolder.usage, cacheHolder.region, cacheHolder.cacheLazy
				);
			}
			else {
				super.setCollectionCacheConcurrencyStrategy( cacheHolder.role, cacheHolder.usage, cacheHolder.region );
			}
		}
		caches.clear();
		try {
			inSecondPass = true;
			Iterator iter = secondPasses.iterator();
			while ( iter.hasNext() ) {
				SecondPass sp = ( SecondPass ) iter.next();
				//do the second pass of simple value types first and remove them
				if ( sp instanceof SetSimpleValueTypeSecondPass ) {
					sp.doSecondPass( classes );
					iter.remove();
				}
			}
			processFkSecondPassInOrder();
			iter = secondPasses.iterator();
			while ( iter.hasNext() ) {
				SecondPass sp = ( SecondPass ) iter.next();
				//do the second pass of fk before the others and remove them
				if ( sp instanceof CreateKeySecondPass ) {
					sp.doSecondPass( classes );
					iter.remove();
				}
			}

			iter = secondPasses.iterator();
			while ( iter.hasNext() ) {
				SecondPass sp = ( SecondPass ) iter.next();
				//do the SecondaryTable second pass before any association becasue associations can be built on joins
				if ( sp instanceof SecondaryTableSecondPass ) {
					sp.doSecondPass( classes );
					iter.remove();
				}
			}
			super.secondPassCompile();
			inSecondPass = false;
		}
		catch ( RecoverableException e ) {
			//the exception was not recoverable after all
			throw ( RuntimeException ) e.getCause();
		}

		Iterator<Map.Entry<Table,List<UniqueConstraintHolder>>> tables = uniqueConstraintHoldersByTable.entrySet().iterator();
		while ( tables.hasNext() ) {
			final Map.Entry<Table,List<UniqueConstraintHolder>> entry = tables.next();
			final Table table = entry.getKey();
			final List<UniqueConstraintHolder> uniqueConstraints = entry.getValue();
			int uniqueIndexPerTable = 0;
			for ( UniqueConstraintHolder holder : uniqueConstraints ) {
				uniqueIndexPerTable++;
				final String keyName = StringHelper.isEmpty( holder.getName() )
						? "key" + uniqueIndexPerTable
						: holder.getName();
				buildUniqueKeyFromColumnNames( table, keyName, holder.getColumns() );
			}
		}
		applyConstraintsToDDL();
	}

	private void applyConstraintsToDDL() {
		boolean applyOnDdl = getProperties().getProperty(
				"hibernate.validator.apply_to_ddl",
				"true"
		)
				.equalsIgnoreCase( "true" );

		if ( !applyOnDdl ) {
			return; // nothing to do in this case
		}
		applyHibernateValidatorLegacyConstraintsOnDDL();
		applyBeanValidationConstraintsOnDDL();
	}

	private void applyHibernateValidatorLegacyConstraintsOnDDL() {
		//TODO search for the method only once and cache it?
		Constructor validatorCtr = null;
		Method applyMethod = null;
		try {
			Class classValidator = ReflectHelper.classForName(
					"org.hibernate.validator.ClassValidator", this.getClass()
			);
			Class messageInterpolator = ReflectHelper.classForName(
					"org.hibernate.validator.MessageInterpolator", this.getClass()
			);
			validatorCtr = classValidator.getDeclaredConstructor(
					Class.class, ResourceBundle.class, messageInterpolator, Map.class, ReflectionManager.class
			);
			applyMethod = classValidator.getMethod( "apply", PersistentClass.class );
		}
		catch ( ClassNotFoundException e ) {
			if ( !isValidatorNotPresentLogged ) {
				log.info( "Hibernate Validator not found: ignoring" );
			}
			isValidatorNotPresentLogged = true;
		}
		catch ( NoSuchMethodException e ) {
			throw new AnnotationException( e );
		}
		if ( applyMethod != null ) {
			for ( PersistentClass persistentClazz : ( Collection<PersistentClass> ) classes.values() ) {
				//integrate the validate framework
				String className = persistentClazz.getClassName();
				if ( StringHelper.isNotEmpty( className ) ) {
					try {
						Object validator = validatorCtr.newInstance(
								ReflectHelper.classForName( className ), null, null, null, reflectionManager
						);
						applyMethod.invoke( validator, persistentClazz );
					}
					catch ( Exception e ) {
						log.warn( "Unable to apply constraints on DDL for " + className, e );
					}
				}
			}
		}
	}

	private void applyBeanValidationConstraintsOnDDL() {
		BeanValidationActivator.applyDDL( ( Collection<PersistentClass> ) classes.values(), getProperties() );
	}

	/**
	 * Processes FKSecondPass instances trying to resolve any
	 * graph circularity (ie PK made of a many to one linking to
	 * an entity having a PK made of a ManyToOne ...).
	 */
	private void processFkSecondPassInOrder() {
		log.debug( "processing fk mappings (*ToOne and JoinedSubclass)" );
		List<FkSecondPass> fkSecondPasses = getFKSecondPassesOnly();

		if ( fkSecondPasses.size() == 0 ) {
			return; // nothing to do here
		}

		// split FkSecondPass instances into primary key and non primary key FKs.
		// While doing so build a map of class names to FkSecondPass instances depending on this class.
		Map<String, Set<FkSecondPass>> isADependencyOf = new HashMap<String, Set<FkSecondPass>>();
		List endOfQueueFkSecondPasses = new ArrayList( fkSecondPasses.size() );
		for ( FkSecondPass sp : fkSecondPasses ) {
			if ( sp.isInPrimaryKey() ) {
				String referenceEntityName = sp.getReferencedEntityName();
				PersistentClass classMapping = getClassMapping( referenceEntityName );
				String dependentTable = classMapping.getTable().getQuotedName();
				if ( !isADependencyOf.containsKey( dependentTable ) ) {
					isADependencyOf.put( dependentTable, new HashSet<FkSecondPass>() );
				}
				isADependencyOf.get( dependentTable ).add( sp );
			}
			else {
				endOfQueueFkSecondPasses.add( sp );
			}
		}

		// using the isADependencyOf map we order the FkSecondPass recursively instances into the right order for processing
		List<FkSecondPass> orderedFkSecondPasses = new ArrayList( fkSecondPasses.size() );
		for ( String tableName : isADependencyOf.keySet() ) {
			buildRecursiveOrderedFkSecondPasses( orderedFkSecondPasses, isADependencyOf, tableName, tableName );
		}

		// process the ordered FkSecondPasses
		for ( FkSecondPass sp : orderedFkSecondPasses ) {
			sp.doSecondPass( classes );
		}

		processEndOfQueue( endOfQueueFkSecondPasses );
	}

	private void processEndOfQueue(List endOfQueueFkSecondPasses) {
		/*
		 * If a second pass raises a recoverableException, queue it for next round
		 * stop of no pass has to be processed or if the number of pass to processes
		 * does not diminish between two rounds.
		 * If some failing pass remain, raise the original exception
		 */
		boolean stopProcess = false;
		RuntimeException originalException = null;
		while ( !stopProcess ) {
			List failingSecondPasses = new ArrayList();
			Iterator it = endOfQueueFkSecondPasses.listIterator();
			while ( it.hasNext() ) {
				final SecondPass pass = ( SecondPass ) it.next();
				try {
					pass.doSecondPass( classes );
				}
				catch ( RecoverableException e ) {
					failingSecondPasses.add( pass );
					if ( originalException == null ) {
						originalException = ( RuntimeException ) e.getCause();
					}
				}
			}
			stopProcess = failingSecondPasses.size() == 0 || failingSecondPasses.size() == endOfQueueFkSecondPasses.size();
			endOfQueueFkSecondPasses = failingSecondPasses;
		}
		if ( endOfQueueFkSecondPasses.size() > 0 ) {
			throw originalException;
		}
	}

	/**
	 * @return Returns a list of all <code>secondPasses</code> instances which are a instance of
	 *         <code>FkSecondPass</code>.
	 */
	private List<FkSecondPass> getFKSecondPassesOnly() {
		Iterator iter = secondPasses.iterator();
		List<FkSecondPass> fkSecondPasses = new ArrayList<FkSecondPass>( secondPasses.size() );
		while ( iter.hasNext() ) {
			SecondPass sp = ( SecondPass ) iter.next();
			//do the second pass of fk before the others and remove them
			if ( sp instanceof FkSecondPass ) {
				fkSecondPasses.add( ( FkSecondPass ) sp );
				iter.remove();
			}
		}
		return fkSecondPasses;
	}

	/**
	 * Recursively builds a list of FkSecondPass instances ready to be processed in this order.
	 * Checking all dependencies recursively seems quite expensive, but the original code just relied
	 * on some sort of table name sorting which failed in certain circumstances.
	 * <p/>
	 * See <tt>ANN-722</tt> and <tt>ANN-730</tt>
	 *
	 * @param orderedFkSecondPasses The list containing the <code>FkSecondPass<code> instances ready
	 * for processing.
	 * @param isADependencyOf Our lookup data structure to determine dependencies between tables
	 * @param startTable Table name to start recursive algorithm.
	 * @param currentTable The current table name used to check for 'new' dependencies.
	 */
	private void buildRecursiveOrderedFkSecondPasses(
			List orderedFkSecondPasses,
			Map<String, Set<FkSecondPass>> isADependencyOf, String startTable, String currentTable) {

		Set<FkSecondPass> dependencies = isADependencyOf.get( currentTable );

		// bottom out
		if ( dependencies == null || dependencies.size() == 0 ) {
			return;
		}

		for ( FkSecondPass sp : dependencies ) {
			String dependentTable = sp.getValue().getTable().getQuotedName();
			if ( dependentTable.compareTo( startTable ) == 0 ) {
				StringBuilder sb = new StringBuilder(
						"Foreign key circularity dependency involving the following tables: "
				);
				throw new AnnotationException( sb.toString() );
			}
			buildRecursiveOrderedFkSecondPasses( orderedFkSecondPasses, isADependencyOf, startTable, dependentTable );
			if ( !orderedFkSecondPasses.contains( sp ) ) {
				orderedFkSecondPasses.add( 0, sp );
			}
		}
	}

	private void processArtifactsOfType(String artifact) {
		if ( "hbm".equalsIgnoreCase( artifact ) ) {
			log.debug( "Process hbm files" );
			for ( Document document : hbmDocuments ) {
				super.add( document );
			}
			hbmDocuments.clear();
			hbmEntities.clear();
		}
		else if ( "class".equalsIgnoreCase( artifact ) ) {
			log.debug( "Process annotated classes" );
			//bind classes in the correct order calculating some inheritance state
			List<XClass> orderedClasses = orderAndFillHierarchy( annotatedClasses );
			Map<XClass, InheritanceState> inheritanceStatePerClass = AnnotationBinder.buildInheritanceStates(
					orderedClasses, reflectionManager
			);
			ExtendedMappings mappings = createExtendedMappings();

			for ( XClass clazz : orderedClasses ) {
				//todo use the same extended mapping
				AnnotationBinder.bindClass( clazz, inheritanceStatePerClass, mappings );
			}
			annotatedClasses.clear();
			annotatedClassEntities.clear();
		}
		else {
			log.warn( "Unknown artifact: {}", artifact );
		}
	}

	private void removeConflictedArtifact(String artifact) {
		if ( "hbm".equalsIgnoreCase( artifact ) ) {
			for ( String entity : hbmEntities.keySet() ) {
				if ( annotatedClassEntities.containsKey( entity ) ) {
					annotatedClasses.remove( annotatedClassEntities.get( entity ) );
					annotatedClassEntities.remove( entity );
				}
			}
		}
		else if ( "class".equalsIgnoreCase( artifact ) ) {
			for ( String entity : annotatedClassEntities.keySet() ) {
				if ( hbmEntities.containsKey( entity ) ) {
					hbmDocuments.remove( hbmEntities.get( entity ) );
					hbmEntities.remove( entity );
				}
			}
		}
	}

	private void buildUniqueKeyFromColumnNames(Table table, String keyName, String[] columnNames) {
		ExtendedMappings mappings = createExtendedMappings();
		keyName = mappings.getObjectNameNormalizer().normalizeIdentifierQuoting( keyName );

		UniqueKey uc;
		int size = columnNames.length;
		Column[] columns = new Column[size];
		Set<Column> unbound = new HashSet<Column>();
		Set<Column> unboundNoLogical = new HashSet<Column>();
		for ( int index = 0; index < size; index++ ) {
			final String logicalColumnName = mappings.getObjectNameNormalizer()
					.normalizeIdentifierQuoting( columnNames[index] );
			try {
				final String columnName = mappings.getPhysicalColumnName( logicalColumnName, table );
				columns[index] = new Column( columnName );
				unbound.add( columns[index] );
				//column equals and hashcode is based on column name
			}
			catch ( MappingException e ) {
				unboundNoLogical.add( new Column( logicalColumnName ) );
			}
		}
		for ( Column column : columns ) {
			if ( table.containsColumn( column ) ) {
				uc = table.getOrCreateUniqueKey( keyName );
				uc.addColumn( table.getColumn( column ) );
				unbound.remove( column );
			}
		}
		if ( unbound.size() > 0 || unboundNoLogical.size() > 0 ) {
			StringBuilder sb = new StringBuilder( "Unable to create unique key constraint (" );
			for ( String columnName : columnNames ) {
				sb.append( columnName ).append( ", " );
			}
			sb.setLength( sb.length() - 2 );
			sb.append( ") on table " ).append( table.getName() ).append( ": " );
			for ( Column column : unbound ) {
				sb.append( column.getName() ).append( ", " );
			}
			for ( Column column : unboundNoLogical ) {
				sb.append( column.getName() ).append( ", " );
			}
			sb.setLength( sb.length() - 2 );
			sb.append( " not found" );
			throw new AnnotationException( sb.toString() );
		}
	}

	@Override
	protected void parseMappingElement(Element subelement, String name) {
		Attribute rsrc = subelement.attribute( "resource" );
		Attribute file = subelement.attribute( "file" );
		Attribute jar = subelement.attribute( "jar" );
		Attribute pckg = subelement.attribute( "package" );
		Attribute clazz = subelement.attribute( "class" );
		if ( rsrc != null ) {
			log.debug( "{} <- {}", name, rsrc );
			addResource( rsrc.getValue() );
		}
		else if ( jar != null ) {
			log.debug( "{} <- {}", name, jar );
			addJar( new File( jar.getValue() ) );
		}
		else if ( file != null ) {
			log.debug( "{} <- {}", name, file );
			addFile( file.getValue() );
		}
		else if ( pckg != null ) {
			log.debug( "{} <- {}", name, pckg );
			addPackage( pckg.getValue() );
		}
		else if ( clazz != null ) {
			log.debug( "{} <- {}", name, clazz );
			Class loadedClass;
			try {
				loadedClass = ReflectHelper.classForName( clazz.getValue() );
			}
			catch ( ClassNotFoundException cnf ) {
				throw new MappingException(
						"Unable to load class declared as <mapping class=\"" + clazz.getValue() + "\"/> in the configuration:",
						cnf
				);
			}
			catch ( NoClassDefFoundError ncdf ) {
				throw new MappingException(
						"Unable to load class declared as <mapping class=\"" + clazz.getValue() + "\"/> in the configuration:",
						ncdf
				);
			}

			addAnnotatedClass( loadedClass );
		}
		else {
			throw new MappingException( "<mapping> element in configuration specifies no attributes" );
		}
	}

	@Override
	protected void add(org.dom4j.Document doc) throws MappingException {
		boolean ejb3Xml = "entity-mappings".equals( doc.getRootElement().getName() );
		if ( inSecondPass ) {
			//if in second pass bypass the queueing, getExtendedQueue reuse this method
			if ( !ejb3Xml ) {
				super.add( doc );
			}
		}
		else {
			if ( !ejb3Xml ) {
				final Element hmNode = doc.getRootElement();
				Attribute packNode = hmNode.attribute( "package" );
				String defaultPackage = packNode != null
						? packNode.getValue()
						: "";
				Set<String> entityNames = new HashSet<String>();
				findClassNames( defaultPackage, hmNode, entityNames );
				for ( String entity : entityNames ) {
					hbmEntities.put( entity, doc );
				}
				hbmDocuments.add( doc );
			}
			else {
				final MetadataProvider metadataProvider = ( ( MetadataProviderInjector ) reflectionManager ).getMetadataProvider();
				JPAMetadataProvider jpaMetadataProvider = ( JPAMetadataProvider ) metadataProvider;
				List<String> classnames = jpaMetadataProvider.getXMLContext().addDocument( doc );
				for ( String classname : classnames ) {
					try {
						annotatedClasses.add( reflectionManager.classForName( classname, this.getClass() ) );
					}
					catch ( ClassNotFoundException e ) {
						throw new AnnotationException( "Unable to load class defined in XML: " + classname, e );
					}
				}
			}
		}
	}

	private static void findClassNames(
			String defaultPackage, final Element startNode,
			final java.util.Set names
	) {
		// if we have some extends we need to check if those classes possibly could be inside the
		// same hbm.xml file...
		Iterator[] classes = new Iterator[4];
		classes[0] = startNode.elementIterator( "class" );
		classes[1] = startNode.elementIterator( "subclass" );
		classes[2] = startNode.elementIterator( "joined-subclass" );
		classes[3] = startNode.elementIterator( "union-subclass" );

		Iterator classIterator = new JoinedIterator( classes );
		while ( classIterator.hasNext() ) {
			Element element = ( Element ) classIterator.next();
			String entityName = element.attributeValue( "entity-name" );
			if ( entityName == null ) {
				entityName = getClassName( element.attribute( "name" ), defaultPackage );
			}
			names.add( entityName );
			findClassNames( defaultPackage, element, names );
		}
	}

	private static String getClassName(Attribute name, String defaultPackage) {
		if ( name == null ) {
			return null;
		}
		String unqualifiedName = name.getValue();
		if ( unqualifiedName == null ) {
			return null;
		}
		if ( unqualifiedName.indexOf( '.' ) < 0 && defaultPackage != null ) {
			return defaultPackage + '.' + unqualifiedName;
		}
		return unqualifiedName;
	}

	public void setPrecedence(String precedence) {
		this.precedence = precedence;
	}

	private static class CacheHolder {
		public CacheHolder(String role, String usage, String region, boolean isClass, boolean cacheLazy) {
			this.role = role;
			this.usage = usage;
			this.region = region;
			this.isClass = isClass;
			this.cacheLazy = cacheLazy;
		}

		public String role;
		public String usage;
		public String region;
		public boolean isClass;
		public boolean cacheLazy;
	}

	@Override
	public AnnotationConfiguration addInputStream(InputStream xmlInputStream) throws MappingException {
		try {
			List errors = new ArrayList();
			SAXReader saxReader = xmlHelper.createSAXReader( "XML InputStream", errors, getEntityResolver() );
			try {
				saxReader.setFeature( "http://apache.org/xml/features/validation/schema", true );
				//saxReader.setFeature( "http://apache.org/xml/features/validation/dynamic", true );
				//set the default schema locators
				saxReader.setProperty(
						"http://apache.org/xml/properties/schema/external-schemaLocation",
						"http://java.sun.com/xml/ns/persistence/orm orm_1_0.xsd"
				);
			}
			catch ( SAXException e ) {
				saxReader.setValidation( false );
			}
			org.dom4j.Document doc = saxReader
					.read( new InputSource( xmlInputStream ) );

			if ( errors.size() != 0 ) {
				throw new MappingException( "invalid mapping", ( Throwable ) errors.get( 0 ) );
			}
			add( doc );
			return this;
		}
		catch ( DocumentException e ) {
			throw new MappingException( "Could not parse mapping document in input stream", e );
		}
		finally {
			try {
				xmlInputStream.close();
			}
			catch ( IOException ioe ) {
				log.warn( "Could not close input stream", ioe );
			}
		}
	}

	public SessionFactory buildSessionFactory() throws HibernateException {
		enableLegacyHibernateValidator();
		enableBeanValidation();
		enableHibernateSearch();
		return super.buildSessionFactory();
	}

	private void enableLegacyHibernateValidator() {
		//add validator events if the jar is available
		boolean enableValidatorListeners = !"false".equalsIgnoreCase(
				getProperty(
						"hibernate.validator.autoregister_listeners"
				)
		);
		Class validateEventListenerClass = null;
		try {
			validateEventListenerClass = ReflectHelper.classForName(
					"org.hibernate.validator.event.ValidateEventListener",
					AnnotationConfiguration.class
			);
		}
		catch ( ClassNotFoundException e ) {
			//validator is not present
			log.debug( "Legacy Validator not present in classpath, ignoring event listener registration" );
		}
		if ( enableValidatorListeners && validateEventListenerClass != null ) {
			//TODO so much duplication
			Object validateEventListener;
			try {
				validateEventListener = validateEventListenerClass.newInstance();
			}
			catch ( Exception e ) {
				throw new AnnotationException( "Unable to load Validator event listener", e );
			}
			{
				boolean present = false;
				PreInsertEventListener[] listeners = getEventListeners().getPreInsertEventListeners();
				if ( listeners != null ) {
					for ( Object eventListener : listeners ) {
						//not isAssignableFrom since the user could subclass
						present = present || validateEventListenerClass == eventListener.getClass();
					}
					if ( !present ) {
						int length = listeners.length + 1;
						PreInsertEventListener[] newListeners = new PreInsertEventListener[length];
						System.arraycopy( listeners, 0, newListeners, 0, length - 1 );
						newListeners[length - 1] = ( PreInsertEventListener ) validateEventListener;
						getEventListeners().setPreInsertEventListeners( newListeners );
					}
				}
				else {
					getEventListeners().setPreInsertEventListeners(
							new PreInsertEventListener[] { ( PreInsertEventListener ) validateEventListener }
					);
				}
			}

			//update event listener
			{
				boolean present = false;
				PreUpdateEventListener[] listeners = getEventListeners().getPreUpdateEventListeners();
				if ( listeners != null ) {
					for ( Object eventListener : listeners ) {
						//not isAssignableFrom since the user could subclass
						present = present || validateEventListenerClass == eventListener.getClass();
					}
					if ( !present ) {
						int length = listeners.length + 1;
						PreUpdateEventListener[] newListeners = new PreUpdateEventListener[length];
						System.arraycopy( listeners, 0, newListeners, 0, length - 1 );
						newListeners[length - 1] = ( PreUpdateEventListener ) validateEventListener;
						getEventListeners().setPreUpdateEventListeners( newListeners );
					}
				}
				else {
					getEventListeners().setPreUpdateEventListeners(
							new PreUpdateEventListener[] { ( PreUpdateEventListener ) validateEventListener }
					);
				}
			}
		}
	}

	private void enableBeanValidation() {
		BeanValidationActivator.activateBeanValidation( getEventListeners(), getProperties() );
	}

	/**
	 * Tries to automatically register Hibernate Search event listeners by locating the
	 * appropriate bootstrap class and calling the <code>enableHibernateSearch</code> method.
	 */
	private void enableHibernateSearch() {
		// load the bootstrap class
		Class searchStartupClass;
		try {
			searchStartupClass = ReflectHelper.classForName( SEARCH_STARTUP_CLASS, AnnotationConfiguration.class );
		}
		catch ( ClassNotFoundException e ) {
			// TODO remove this together with SearchConfiguration after 3.1.0 release of Search
			// try loading deprecated HibernateSearchEventListenerRegister
			try {
				searchStartupClass = ReflectHelper.classForName(
						"org.hibernate.cfg.search.HibernateSearchEventListenerRegister", AnnotationConfiguration.class
				);
			}
			catch ( ClassNotFoundException cnfe ) {
				log.debug( "Search not present in classpath, ignoring event listener registration." );
				return;
			}
		}

		// call the method for registering the listeners
		try {
			Object searchStartupInstance = searchStartupClass.newInstance();
			Method enableSearchMethod = searchStartupClass.getDeclaredMethod(
					SEARCH_STARTUP_METHOD,
					EventListeners.class, Properties.class
			);
			enableSearchMethod.invoke( searchStartupInstance, getEventListeners(), getProperties() );
		}
		catch ( InstantiationException e ) {
			log.debug( "Unable to instantiate {}, ignoring event listener registration.", SEARCH_STARTUP_CLASS );
		}
		catch ( IllegalAccessException e ) {
			log.debug( "Unable to instantiate {}, ignoring event listener registration.", SEARCH_STARTUP_CLASS );
		}
		catch ( NoSuchMethodException e ) {
			log.debug( "Method enableHibernateSearch() not found in {}.", SEARCH_STARTUP_CLASS );
		}
		catch ( InvocationTargetException e ) {
			log.debug( "Unable to execute {}, ignoring event listener registration.", SEARCH_STARTUP_METHOD );
		}
	}

	@Override
	public AnnotationConfiguration addFile(String xmlFile) throws MappingException {
		super.addFile( xmlFile );
		return this;
	}

	@Override
	public AnnotationConfiguration addFile(File xmlFile) throws MappingException {
		super.addFile( xmlFile );
		return this;
	}

	@Override
	public AnnotationConfiguration addCacheableFile(File xmlFile) throws MappingException {
		super.addCacheableFile( xmlFile );
		return this;
	}

	@Override
	public AnnotationConfiguration addCacheableFile(String xmlFile) throws MappingException {
		super.addCacheableFile( xmlFile );
		return this;
	}

	@Override
	public AnnotationConfiguration addXML(String xml) throws MappingException {
		super.addXML( xml );
		return this;
	}

	@Override
	public AnnotationConfiguration addURL(URL url) throws MappingException {
		super.addURL( url );
		return this;
	}

	@Override
	public AnnotationConfiguration addResource(String resourceName, ClassLoader classLoader) throws MappingException {
		super.addResource( resourceName, classLoader );
		return this;
	}

	@Override
	public AnnotationConfiguration addDocument(org.w3c.dom.Document doc) throws MappingException {
		super.addDocument( doc );
		return this;
	}

	@Override
	public AnnotationConfiguration addResource(String resourceName) throws MappingException {
		super.addResource( resourceName );
		return this;
	}

	@Override
	public AnnotationConfiguration addClass(Class persistentClass) throws MappingException {
		super.addClass( persistentClass );
		return this;
	}

	@Override
	public AnnotationConfiguration addJar(File jar) throws MappingException {
		super.addJar( jar );
		return this;
	}

	@Override
	public AnnotationConfiguration addDirectory(File dir) throws MappingException {
		super.addDirectory( dir );
		return this;
	}

	@Override
	public AnnotationConfiguration setInterceptor(Interceptor interceptor) {
		super.setInterceptor( interceptor );
		return this;
	}

	@Override
	public AnnotationConfiguration setProperties(Properties properties) {
		super.setProperties( properties );
		return this;
	}

	@Override
	public AnnotationConfiguration addProperties(Properties extraProperties) {
		super.addProperties( extraProperties );
		return this;
	}

	@Override
	public AnnotationConfiguration mergeProperties(Properties properties) {
		super.mergeProperties( properties );
		return this;
	}

	@Override
	public AnnotationConfiguration setProperty(String propertyName, String value) {
		super.setProperty( propertyName, value );
		return this;
	}

	@Override
	public AnnotationConfiguration configure() throws HibernateException {
		super.configure();
		return this;
	}

	@Override
	public AnnotationConfiguration configure(String resource) throws HibernateException {
		super.configure( resource );
		return this;
	}

	@Override
	public AnnotationConfiguration configure(URL url) throws HibernateException {
		super.configure( url );
		return this;
	}

	@Override
	public AnnotationConfiguration configure(File configFile) throws HibernateException {
		super.configure( configFile );
		return this;
	}

	@Override
	protected AnnotationConfiguration doConfigure(InputStream stream, String resourceName) throws HibernateException {
		super.doConfigure( stream, resourceName );
		return this;
	}

	@Override
	public AnnotationConfiguration configure(org.w3c.dom.Document document) throws HibernateException {
		super.configure( document );
		return this;
	}

	@Override
	protected AnnotationConfiguration doConfigure(Document doc) throws HibernateException {
		super.doConfigure( doc );
		return this;
	}

	@Override
	public AnnotationConfiguration setCacheConcurrencyStrategy(String clazz, String concurrencyStrategy)
			throws MappingException {
		super.setCacheConcurrencyStrategy( clazz, concurrencyStrategy );
		return this;
	}

	@Override
	public AnnotationConfiguration setCollectionCacheConcurrencyStrategy(String collectionRole, String concurrencyStrategy)
			throws MappingException {
		super.setCollectionCacheConcurrencyStrategy( collectionRole, concurrencyStrategy );
		return this;
	}

	@Override
	public AnnotationConfiguration setNamingStrategy(NamingStrategy namingStrategy) {
		super.setNamingStrategy( namingStrategy );
		return this;
	}

	//not a public API
	public ReflectionManager getReflectionManager() {
		return reflectionManager;
	}

	protected class ExtendedMappingsImpl extends MappingsImpl implements ExtendedMappings {
		public void addDefaultGenerator(IdGenerator generator) {
			this.addGenerator( generator );
			defaultNamedGenerators.add( generator.getName() );
		}

		public boolean isInSecondPass() {
			return inSecondPass;
		}

		public IdGenerator getGenerator(String name) {
			return getGenerator( name, null );
		}

		public IdGenerator getGenerator(String name, Map<String, IdGenerator> localGenerators) {
			if ( localGenerators != null ) {
				IdGenerator result = localGenerators.get( name );
				if ( result != null ) {
					return result;
				}
			}
			return namedGenerators.get( name );
		}

		public void addGenerator(IdGenerator generator) {
			if ( !defaultNamedGenerators.contains( generator.getName() ) ) {
				IdGenerator old = namedGenerators.put( generator.getName(), generator );
				if ( old != null ) {
					log.warn( "duplicate generator name {}", old.getName() );
				}
			}
		}

		public void addGeneratorTable(String name, Properties params) {
			Object old = generatorTables.put( name, params );
			if ( old != null ) {
				log.warn( "duplicate generator table: {}", name );
			}
		}

		public Properties getGeneratorTableProperties(String name, Map<String, Properties> localGeneratorTables) {
			if ( localGeneratorTables != null ) {
				Properties result = localGeneratorTables.get( name );
				if ( result != null ) {
					return result;
				}
			}
			return generatorTables.get( name );
		}

		public Map<String, Join> getJoins(String entityName) {
			return joins.get( entityName );
		}

		public void addJoins(PersistentClass persistentClass, Map<String, Join> joins) {
			Object old = AnnotationConfiguration.this.joins.put( persistentClass.getEntityName(), joins );
			if ( old != null ) {
				log.warn( "duplicate joins for class: {}", persistentClass.getEntityName() );
			}
		}

		public AnnotatedClassType getClassType(XClass clazz) {
			AnnotatedClassType type = classTypes.get( clazz.getName() );
			if ( type == null ) {
				return addClassType( clazz );
			}
			else {
				return type;
			}
		}

		//FIXME should be private but is part of the ExtendedMapping contract
		public AnnotatedClassType addClassType(XClass clazz) {
			AnnotatedClassType type;
			if ( clazz.isAnnotationPresent( Entity.class ) ) {
				type = AnnotatedClassType.ENTITY;
			}
			else if ( clazz.isAnnotationPresent( Embeddable.class ) ) {
				type = AnnotatedClassType.EMBEDDABLE;
			}
			else if ( clazz.isAnnotationPresent( MappedSuperclass.class ) ) {
				type = AnnotatedClassType.EMBEDDABLE_SUPERCLASS;
			}
			else {
				type = AnnotatedClassType.NONE;
			}
			classTypes.put( clazz.getName(), type );
			return type;
		}

		/**
		 * {@inheritDoc}
		 */
		public Map<Table, List<String[]>> getTableUniqueConstraints() {
			final Map<Table, List<String[]>> deprecatedStructure = new HashMap<Table, List<String[]>>(
					CollectionHelper.determineProperSizing( getUniqueConstraintHoldersByTable() ),
					CollectionHelper.LOAD_FACTOR
			);
			for ( Map.Entry<Table, List<UniqueConstraintHolder>> entry : getUniqueConstraintHoldersByTable().entrySet() ) {
				List<String[]> columnsPerConstraint = new ArrayList<String[]>(
						CollectionHelper.determineProperSizing( entry.getValue().size() )
				);
				deprecatedStructure.put( entry.getKey(), columnsPerConstraint );
				for ( UniqueConstraintHolder holder : entry.getValue() ) {
					columnsPerConstraint.add( holder.getColumns() );
				}
			}
			return deprecatedStructure;
		}

		/**
		 * {@inheritDoc}
		 */
		public Map<Table, List<UniqueConstraintHolder>> getUniqueConstraintHoldersByTable() {
			return uniqueConstraintHoldersByTable;
		}

		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings({ "unchecked" })
		public void addUniqueConstraints(Table table, List uniqueConstraints) {
			List<UniqueConstraintHolder> constraintHolders = new ArrayList<UniqueConstraintHolder>(
					CollectionHelper.determineProperSizing( uniqueConstraints.size() )
			);

			int keyNameBase = determineCurrentNumberOfUniqueConstraintHolders( table );
			for ( String[] columns : (List<String[]>)uniqueConstraints ) {
				final String keyName = "key" + keyNameBase++;
				constraintHolders.add(
						new UniqueConstraintHolder().setName( keyName ).setColumns( columns )
				);
			}
			addUniqueConstraintHolders( table, constraintHolders );
		}

		private int determineCurrentNumberOfUniqueConstraintHolders(Table table) {
			List currentHolders = getUniqueConstraintHoldersByTable().get( table );
			return currentHolders == null
					? 0
					: currentHolders.size();
		}

		/**
		 * {@inheritDoc}
		 */
		public void addUniqueConstraintHolders(Table table, List<UniqueConstraintHolder> uniqueConstraintHolders) {
			List<UniqueConstraintHolder> holderList = getUniqueConstraintHoldersByTable().get( table );
			if ( holderList == null ) {
				holderList = new ArrayList<UniqueConstraintHolder>();
				getUniqueConstraintHoldersByTable().put( table, holderList );
			}
			holderList.addAll( uniqueConstraintHolders );
		}

		public void addMappedBy(String entityName, String propertyName, String inversePropertyName) {
			mappedByResolver.put( entityName + "." + propertyName, inversePropertyName );
		}

		public String getFromMappedBy(String entityName, String propertyName) {
			return mappedByResolver.get( entityName + "." + propertyName );
		}

		public void addPropertyReferencedAssociation(String entityName, String propertyName, String propertyRef) {
			propertyRefResolver.put( entityName + "." + propertyName, propertyRef );
		}

		public String getPropertyReferencedAssociation(String entityName, String propertyName) {
			return propertyRefResolver.get( entityName + "." + propertyName );
		}

		@Override
		public void addUniquePropertyReference(String referencedClass, String propertyName) {
			super.addUniquePropertyReference( referencedClass, propertyName );
		}

		@Override
		public void addPropertyReference(String referencedClass, String propertyName) {
			super.addPropertyReference( referencedClass, propertyName );
		}

		public ReflectionManager getReflectionManager() {
			return reflectionManager;
		}

		public void addDefaultQuery(String name, NamedQueryDefinition query) {
			super.addQuery( name, query );
			defaultNamedQueryNames.add( name );
		}

		public void addDefaultSQLQuery(String name, NamedSQLQueryDefinition query) {
			super.addSQLQuery( name, query );
			defaultNamedNativeQueryNames.add( name );
		}

		public void addDefaultResultSetMapping(ResultSetMappingDefinition definition) {
			final String name = definition.getName();
			if ( !defaultSqlResulSetMappingNames.contains( name )
					&& super.getResultSetMapping( name ) != null ) {
				removeResultSetMapping( name );
			}
			super.addResultSetMapping( definition );
			defaultSqlResulSetMappingNames.add( name );
		}

		@Override
		public void addQuery(String name, NamedQueryDefinition query) throws DuplicateMappingException {
			if ( !defaultNamedQueryNames.contains( name ) ) {
				super.addQuery( name, query );
			}
		}

		@Override
		public void addResultSetMapping(ResultSetMappingDefinition definition) throws DuplicateMappingException {
			if ( !defaultSqlResulSetMappingNames.contains( definition.getName() ) ) {
				super.addResultSetMapping( definition );
			}
		}

		@Override
		public void addSQLQuery(String name, NamedSQLQueryDefinition query) throws DuplicateMappingException {
			if ( !defaultNamedNativeQueryNames.contains( name ) ) {
				super.addSQLQuery( name, query );
			}
		}

		public Map getClasses() {
			return classes;
		}

		public void addAnyMetaDef(AnyMetaDef defAnn) throws AnnotationException {
			if ( anyMetaDefs.containsKey( defAnn.name() ) ) {
				throw new AnnotationException( "Two @AnyMetaDef with the same name defined: " + defAnn.name() );
			}
			anyMetaDefs.put( defAnn.name(), defAnn );
		}

		public AnyMetaDef getAnyMetaDef(String name) {
			return anyMetaDefs.get( name );
		}
	}
}
