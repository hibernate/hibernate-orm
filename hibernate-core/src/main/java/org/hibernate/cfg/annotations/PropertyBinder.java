/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Version;
import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.annotations.AttributeBinderType;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AccessType;
import org.hibernate.cfg.AnnotatedColumns;
import org.hibernate.cfg.AnnotationBinder;
import org.hibernate.cfg.AnnotatedColumn;
import org.hibernate.cfg.InheritanceState;
import org.hibernate.cfg.PropertyHolder;
import org.hibernate.cfg.PropertyPreloadedData;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.tuple.AnnotationGenerator;
import org.hibernate.tuple.Generator;
import org.hibernate.tuple.AttributeBinder;
import org.hibernate.tuple.InDatabaseGenerator;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.tuple.ValueGeneration;
import org.hibernate.tuple.ValueGenerator;
import org.jboss.logging.Logger;

import java.lang.annotation.Annotation;
import java.util.Map;

import static org.hibernate.cfg.BinderHelper.getMappedSuperclassOrNull;
import static org.hibernate.cfg.annotations.HCANNHelper.findContainingAnnotation;
import static org.hibernate.internal.util.StringHelper.qualify;

/**
 * @author Emmanuel Bernard
 */
public class PropertyBinder {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, PropertyBinder.class.getName());

	private MetadataBuildingContext buildingContext;

	private String name;
	private String returnedClassName;
	private boolean lazy;
	private String lazyGroup;
	private AccessType accessType;
	private AnnotatedColumns columns;
	private PropertyHolder holder;
	private Value value;
	private boolean insertable = true;
	private boolean updatable = true;
	private String cascade;
	private BasicValueBinder basicValueBinder;
	private XClass declaringClass;
	private boolean declaringClassSet;
	private boolean embedded;
	private EntityBinder entityBinder;
	private boolean isXToMany;
	private String referencedEntityName;
	private PropertyAccessStrategy propertyAccessStrategy;

	public void setReferencedEntityName(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName;
	}

	public void setEmbedded(boolean embedded) {
		this.embedded = embedded;
	}

	public void setEntityBinder(EntityBinder entityBinder) {
		this.entityBinder = entityBinder;
	}

	// property can be null
	// prefer propertyName to property.getName() since some are overloaded
	private XProperty property;
	private XClass returnedClass;
	private boolean isId;
	private Map<XClass, InheritanceState> inheritanceStatePerClass;

	public void setInsertable(boolean insertable) {
		this.insertable = insertable;
	}

	public void setUpdatable(boolean updatable) {
		this.updatable = updatable;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setReturnedClassName(String returnedClassName) {
		this.returnedClassName = returnedClassName;
	}

	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	public void setLazyGroup(String lazyGroup) {
		this.lazyGroup = lazyGroup;
	}

	public void setAccessType(AccessType accessType) {
		this.accessType = accessType;
	}

	public void setColumns(AnnotatedColumns columns) {
		final AnnotatedColumn firstColumn = columns.getColumns().get(0);
		insertable = firstColumn.isInsertable();
		updatable = firstColumn.isUpdatable();
		//consistency is checked later when we know the property name
		this.columns = columns;
	}

	public void setHolder(PropertyHolder holder) {
		this.holder = holder;
	}

	public void setValue(Value value) {
		this.value = value;
	}

	public void setCascade(String cascadeStrategy) {
		this.cascade = cascadeStrategy;
	}

	public void setBuildingContext(MetadataBuildingContext buildingContext) {
		this.buildingContext = buildingContext;
	}

	public void setPropertyAccessStrategy(PropertyAccessStrategy propertyAccessStrategy) {
		this.propertyAccessStrategy = propertyAccessStrategy;
	}

	public void setDeclaringClass(XClass declaringClass) {
		this.declaringClass = declaringClass;
		this.declaringClassSet = true;
	}

	private void validateBind() {
		if ( property.isAnnotationPresent( Immutable.class ) ) {
			throw new AnnotationException( "Property '" + qualify( holder.getPath(), name )
					+ "' may not be '@Immutable'"
					+ " ('@Immutable' may only be applied to entities and collections)" );
		}
		if ( !declaringClassSet ) {
			throw new AssertionFailure( "declaringClass has not been set before a bind" );
		}
	}

	private void validateMake() {
		//TODO check necessary params for a make
	}

	private Property makePropertyAndValue() {
		validateBind();

		LOG.debugf( "MetadataSourceProcessor property %s with lazy=%s", name, lazy );
		final String containerClassName = holder.getClassName();
		holder.startingProperty( property );

		basicValueBinder = new BasicValueBinder( BasicValueBinder.Kind.ATTRIBUTE, buildingContext );
		basicValueBinder.setPropertyName( name );
		basicValueBinder.setReturnedClassName( returnedClassName );
		basicValueBinder.setColumns( columns );
		basicValueBinder.setPersistentClassName( containerClassName );
		basicValueBinder.setType(
				property,
				returnedClass,
				containerClassName,
				holder.resolveAttributeConverterDescriptor( property )
		);
		basicValueBinder.setReferencedEntityName( referencedEntityName );
		basicValueBinder.setAccessType( accessType );


		final SimpleValue propertyValue = basicValueBinder.make();
		setValue( propertyValue );
		return makeProperty();
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void callAttributeBinders(Property prop) {
		final Annotation containingAnnotation = findContainingAnnotation( property, AttributeBinderType.class);
		if ( containingAnnotation != null ) {
			final AttributeBinderType binderAnn = containingAnnotation.annotationType().getAnnotation( AttributeBinderType.class );
			try {
				final AttributeBinder binder = binderAnn.binder().newInstance();
				binder.bind( containingAnnotation, buildingContext, entityBinder.getPersistentClass(), prop );
			}
			catch (Exception e) {
				throw new AnnotationException( "error processing @AttributeBinderType annotation", e );
			}
		}
	}

	//used when value is provided
	public Property makePropertyAndBind() {
		return bind( makeProperty() );
	}

	//used to build everything from scratch
	public Property makePropertyValueAndBind() {
		return bind( makePropertyAndValue() );
	}

	public void setXToMany(boolean xToMany) {
		this.isXToMany = xToMany;
	}

	private Property bind(Property prop) {
		if ( isId ) {
			final RootClass rootClass = (RootClass) holder.getPersistentClass();
			//if an xToMany, it has to be wrapped today.
			//FIXME this poses a problem as the PK is the class instead of the associated class which is not really compliant with the spec
			if ( isXToMany || entityBinder.wrapIdsInEmbeddedComponents() ) {
				Component identifier = (Component) rootClass.getIdentifier();
				if (identifier == null) {
					identifier = AnnotationBinder.createComponent(
							holder,
							new PropertyPreloadedData(null, null, null),
							true,
							false,
							resolveCustomInstantiator( property, returnedClass ),
							buildingContext
					);
					rootClass.setIdentifier( identifier );
					identifier.setNullValue( "undefined" );
					rootClass.setEmbeddedIdentifier( true );
					rootClass.setIdentifierMapper( identifier );
				}
				//FIXME is it good enough?
				identifier.addProperty( prop );
			}
			else {
				rootClass.setIdentifier( (KeyValue) getValue() );
				if ( embedded ) {
					rootClass.setEmbeddedIdentifier( true );
				}
				else {
					rootClass.setIdentifierProperty( prop );
					final org.hibernate.mapping.MappedSuperclass superclass = getMappedSuperclassOrNull(
							declaringClass,
							inheritanceStatePerClass,
							buildingContext
					);
					if ( superclass != null ) {
						superclass.setDeclaredIdentifierProperty(prop);
					}
					else {
						//we know the property is on the actual entity
						rootClass.setDeclaredIdentifierProperty( prop );
					}
				}
			}
		}
		else {
			holder.addProperty( prop, columns, declaringClass );
		}

		callAttributeBinders( prop );

		return prop;
	}

	private Class<? extends EmbeddableInstantiator> resolveCustomInstantiator(XProperty property, XClass embeddableClass) {
		final org.hibernate.annotations.EmbeddableInstantiator propertyAnnotation =
				property.getAnnotation( org.hibernate.annotations.EmbeddableInstantiator.class );
		if ( propertyAnnotation != null ) {
			return propertyAnnotation.value();
		}

		final org.hibernate.annotations.EmbeddableInstantiator classAnnotation =
				embeddableClass.getAnnotation( org.hibernate.annotations.EmbeddableInstantiator.class );
		if ( classAnnotation != null ) {
			return classAnnotation.value();
		}

		return null;
	}

	//used when the value is provided and the binding is done elsewhere
	public Property makeProperty() {
		validateMake();
		LOG.debugf( "Building property %s", name );
		Property property = new Property();
		property.setName( name );
		property.setValue( value );
		property.setLazy( lazy );
		property.setLazyGroup( lazyGroup );
		property.setCascade( cascade );
		property.setPropertyAccessorName( accessType.getType() );
		property.setReturnedClassName( returnedClassName );

		if ( this.property != null ) {
			if ( entityBinder != null ) {
				handleNaturalId( property );
				property.setValueGenerationStrategy( determineValueGenerationStrategy( this.property ) );
			}
			// HHH-4635 -- needed for dialect-specific property ordering
			property.setLob( this.property.isAnnotationPresent( Lob.class ) );
		}

		property.setInsertable( insertable );
		property.setUpdateable( updatable );
		property.setPropertyAccessStrategy( propertyAccessStrategy );

		inferOptimisticLocking(property);

		LOG.tracev( "Cascading {0} with {1}", name, cascade );
		return property;
	}

	private void handleNaturalId(Property prop) {
		NaturalId naturalId = property.getAnnotation(NaturalId.class);
		if ( naturalId != null ) {
			if ( !entityBinder.isRootEntity() ) {
				throw new AnnotationException( "Property '" + qualify( holder.getPath(), name )
						+ "' belongs to an entity subclass and may not be annotated '@NaturalId'" +
						" (only a property of a root '@Entity' or a '@MappedSuperclass' may be a '@NaturalId')" );
			}
			if ( !naturalId.mutable() ) {
				updatable = false;
			}
			prop.setNaturalIdentifier( true );
		}
	}

	private void inferOptimisticLocking(Property prop) {
		// this is already handled for collections in CollectionBinder...
		if ( value instanceof Collection ) {
			prop.setOptimisticLocked( ((Collection) value).isOptimisticLocked() );
		}
		else if ( property != null && property.isAnnotationPresent(OptimisticLock.class) ) {
			final OptimisticLock lockAnn = property.getAnnotation(OptimisticLock.class);
			validateOptimisticLock(lockAnn);
			prop.setOptimisticLocked( !lockAnn.excluded() );
		}
		else {
			prop.setOptimisticLocked( !isToOneValue(value) || insertable ); // && updatable as well???
		}
	}

	private void validateOptimisticLock(OptimisticLock lockAnn) {
		if ( lockAnn.excluded() ) {
			if ( property.isAnnotationPresent(Version.class) ) {
				throw new AnnotationException("Property '" + qualify( holder.getPath(), name )
						+ "' is annotated '@OptimisticLock(excluded=true)' and '@Version'" );
			}
			if ( property.isAnnotationPresent(Id.class) ) {
				throw new AnnotationException("Property '" + qualify( holder.getPath(), name )
						+ "' is annotated '@OptimisticLock(excluded=true)' and '@Id'" );
			}
			if ( property.isAnnotationPresent(EmbeddedId.class) ) {
				throw new AnnotationException("Property '" + qualify( holder.getPath(), name )
						+ "' is annotated '@OptimisticLock(excluded=true)' and '@EmbeddedId'" );
			}
		}
	}

	private Generator determineValueGenerationStrategy(XProperty property) {
		Generator valueGeneration = getValueGenerationFromAnnotations( property );
		if ( valueGeneration == null ) {
			return NoValueGeneration.INSTANCE;
		}
		if ( valueGeneration instanceof InDatabaseGenerator) {
			// if we have an in-db generator, mark it as not insertable nor updatable
			final boolean writable = ( (InDatabaseGenerator) valueGeneration ).writePropertyValue();
			insertable = writable;
			updatable = writable;
		}
		return valueGeneration;
	}

	/**
	 * Returns the value generation strategy for the given property, if any.
	 */
	private Generator getValueGenerationFromAnnotations(XProperty property) {
		Generator valueGeneration = null;
		for ( Annotation annotation : property.getAnnotations() ) {
			final Generator candidate = getValueGenerationFromAnnotation( property, annotation );
			if ( candidate != null ) {
				if ( valueGeneration != null ) {
					throw new AnnotationException( "Property '" + qualify( holder.getPath(), name )
							+ "' has multiple '@ValueGenerationType' annotations" );
				}
				else {
					valueGeneration = candidate;
				}
			}
		}
		return valueGeneration;
	}

	/**
	 * In case the given annotation is a value generator annotation, the corresponding value generation strategy to be
	 * applied to the given property is returned, {@code null} otherwise.
	 */
	private Generator getValueGenerationFromAnnotation(
			XProperty property,
			Annotation annotation) {
		final ValueGenerationType generatorAnnotation = annotation.annotationType().getAnnotation( ValueGenerationType.class );
		if ( generatorAnnotation == null ) {
			return null;
		}

		final Generator valueGeneration =
				instantiateAndInitializeValueGeneration( annotation, generatorAnnotation.generatedBy(), property );

		if ( annotation.annotationType() == Generated.class && property.isAnnotationPresent(Version.class) ) {
			switch ( valueGeneration.getGenerationTiming() ) {
				case INSERT:
					throw new AnnotationException("Property '" + qualify( holder.getPath(), name )
							+ "' is annotated '@Generated(INSERT)' and '@Version' (use '@Generated(ALWAYS)' instead)"

					);
				case UPDATE:
					throw new AnnotationException("Property '" + qualify( holder.getPath(), name )
							+ "' is annotated '@Generated(UPDATE)' and '@Version' (use '@Generated(ALWAYS)' instead)"

					);
			}
		}

		return valueGeneration;
	}

	/**
	 * Instantiates the given generator annotation type, initializing it with the given instance of the corresponding
	 * generator annotation and the property's type.
	 */
	private <A extends Annotation> Generator instantiateAndInitializeValueGeneration(
			A annotation,
			Class<? extends Generator> generationType,
			XProperty property) {

		try {
			final Generator valueGeneration = generationType.newInstance();
			if ( valueGeneration instanceof AnnotationGenerator) {
				// This will cause a CCE in case the generation type doesn't match the annotation type; As this would be
				// a programming error of the generation type developer and thus should show up during testing, we don't
				// check this explicitly; If required, this could be done e.g. using ClassMate
				@SuppressWarnings("unchecked")
				final AnnotationGenerator<A> generation = (AnnotationGenerator<A>) valueGeneration;
				generation.initialize(
						annotation,
						buildingContext.getBootstrapContext().getReflectionManager().toClass( property.getType() ),
						entityBinder.getPersistentClass().getEntityName(),
						property.getName()
				);
			}

			return valueGeneration;
		}
		catch (HibernateException e) {
			throw e;
		}
		catch (Exception e) {
			throw new AnnotationException(
					"Exception occurred during processing of generator annotation: " + qualify(
							holder.getPath(),
							name
					), e
			);
		}
	}

	private static class NoValueGeneration implements ValueGeneration {
		/**
		 * Singleton access
		 */
		public static final NoValueGeneration INSTANCE = new NoValueGeneration();

		@Override
		public GenerationTiming getGenerationTiming() {
			return GenerationTiming.NEVER;
		}

		@Override
		public ValueGenerator<?> getValueGenerator() {
			return null;
		}

		@Override
		public boolean referenceColumnInSql() {
			return true;
		}

		@Override
		public String getDatabaseGeneratedReferencedColumnValue() {
			return null;
		}
	}

	private boolean isToOneValue(Value value) {
		return value instanceof ToOne;
	}

	public void setProperty(XProperty property) {
		this.property = property;
	}

	public void setReturnedClass(XClass returnedClass) {
		this.returnedClass = returnedClass;
	}

	public BasicValueBinder getBasicValueBinder() {
		return basicValueBinder;
	}

	public Value getValue() {
		return value;
	}

	public void setId(boolean id) {
		this.isId = id;
	}

	public boolean isId() {
		return isId;
	}

	public void setInheritanceStatePerClass(Map<XClass, InheritanceState> inheritanceStatePerClass) {
		this.inheritanceStatePerClass = inheritanceStatePerClass;
	}
}
