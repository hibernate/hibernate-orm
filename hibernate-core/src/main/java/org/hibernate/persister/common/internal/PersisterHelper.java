/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.common.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.AttributeConverter;
import javax.persistence.TemporalType;

import org.hibernate.HibernateException;
import org.hibernate.boot.spi.AttributeConverterDescriptor;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.common.spi.AbstractAttribute;
import org.hibernate.persister.common.spi.Attribute;
import org.hibernate.persister.common.spi.AttributeContainer;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.JoinableAttributeContainer;
import org.hibernate.persister.common.spi.ManagedTypeImplementor;
import org.hibernate.persister.common.spi.Table;
import org.hibernate.persister.common.spi.UnionSubclassTable;
import org.hibernate.persister.embeddable.spi.EmbeddableContainer;
import org.hibernate.persister.embeddable.spi.EmbeddablePersister;
import org.hibernate.persister.common.spi.SingularAttribute.Disposition;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.persister.spi.PersisterFactory;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sqm.domain.SqmPluralAttributeElement.ElementClassification;
import org.hibernate.sqm.domain.SqmPluralAttribute.CollectionClassification;
import org.hibernate.sqm.domain.SqmSingularAttribute.SingularAttributeClassification;
import org.hibernate.sqm.domain.SqmSingularAttribute;
import org.hibernate.type.ArrayType;
import org.hibernate.type.BagType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.IdentifierBagType;
import org.hibernate.type.ListType;
import org.hibernate.type.MapType;
import org.hibernate.type.OrderedMapType;
import org.hibernate.type.OrderedSetType;
import org.hibernate.type.SetType;
import org.hibernate.type.SortedMapType;
import org.hibernate.type.SortedSetType;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.MutabilityPlan;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.internal.EntityTypeImpl;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.BasicTypeParameters;
import org.hibernate.type.spi.EmbeddedType;
import org.hibernate.type.spi.EntityType;
import org.hibernate.type.spi.Type;

/**
 * For now mainly a helper for reflection into stuff not exposed on the entity/collection persister
 * contracts
 *
 * @author Steve Ebersole
 */
public class PersisterHelper {
	private final Method subclassTableSpanMethod;
	private final Method subclassPropertyTableNumberMethod;
	private final Method subclassPropertyColumnsMethod;
	private final Method subclassPropertyFormulasMethod;

	/**
	 * Singleton access
	 */
	public static final PersisterHelper INSTANCE = new PersisterHelper();

	private PersisterHelper() {
		try {
			subclassTableSpanMethod = AbstractEntityPersister.class.getDeclaredMethod( "getSubclassTableSpan" );
			subclassTableSpanMethod.setAccessible( true );

			subclassPropertyTableNumberMethod = AbstractEntityPersister.class.getDeclaredMethod( "getSubclassPropertyTableNumber", int.class );
			subclassPropertyTableNumberMethod.setAccessible( true );

			subclassPropertyColumnsMethod = AbstractEntityPersister.class.getDeclaredMethod( "getSubclassPropertyColumnReaderClosure" );
			subclassPropertyColumnsMethod.setAccessible( true );

			subclassPropertyFormulasMethod = AbstractEntityPersister.class.getDeclaredMethod( "getSubclassPropertyFormulaTemplateClosure" );
			subclassPropertyFormulasMethod.setAccessible( true );
		}
		catch (Exception e) {
			throw new HibernateException( "Unable to initialize access to AbstractEntityPersister#getSubclassTableSpan", e );
		}
	}

	public int extractSubclassTableCount(EntityPersister persister) {
		try {
			return (Integer) subclassTableSpanMethod.invoke( persister );
		}
		catch (InvocationTargetException e) {
			throw new HibernateException(
					"Unable to access AbstractEntityPersister#getSubclassTableSpan [" + persister.toString() + "]",
					e.getTargetException()
			);
		}
		catch (Exception e) {
			throw new HibernateException(
					"Unable to access AbstractEntityPersister#getSubclassTableSpan [" + persister.toString() + "]",
					e
			);
		}
	}

	public int getSubclassPropertyTableNumber(EntityPersister persister, int subclassPropertyNumber) {
		try {
			return (Integer) subclassPropertyTableNumberMethod.invoke( persister, subclassPropertyNumber );
		}
		catch (InvocationTargetException e) {
			throw new HibernateException(
					"Unable to access AbstractEntityPersister#getSubclassPropertyTableNumber [" + persister.toString() + "]",
					e.getTargetException()
			);
		}
		catch (Exception e) {
			throw new HibernateException(
					"Unable to access AbstractEntityPersister#getSubclassPropertyTableNumber [" + persister.toString() + "]",
					e
			);
		}
	}

	public Table getPropertyTable(EntityPersister persister, String attributeName, Table[] tables) {
		final String tableName = ( (OuterJoinLoadable) persister ).getPropertyTableName( attributeName );
		for ( Table table : tables ) {
			if ( table instanceof UnionSubclassTable ) {
				if ( ( (UnionSubclassTable) table ).includes( tableName ) ) {
					return table;
				}
			}
			if ( table.getTableExpression().equals( tableName ) ) {
				return table;
			}
		}
		throw new HibernateException(
				"Could not locate Table for attribute [" + persister.getEntityName() + ".'" + attributeName + "]"
		);
	}

	public String[] getSubclassPropertyColumnExpressions(EntityPersister persister, int subclassPropertyNumber) {
		try {
			final String[][] columnExpressions = (String[][]) subclassPropertyColumnsMethod.invoke( persister );
			return columnExpressions[subclassPropertyNumber];
		}
		catch (InvocationTargetException e) {
			throw new HibernateException(
					"Unable to access AbstractEntityPersister#getSubclassPropertyTableNumber [" + persister.toString() + "]",
					e.getTargetException()
			);
		}
		catch (Exception e) {
			throw new HibernateException(
					"Unable to access AbstractEntityPersister#getSubclassPropertyTableNumber [" + persister.toString() + "]",
					e
			);
		}
	}

	public String[] getSubclassPropertyFormulaExpressions(EntityPersister persister, int subclassPropertyNumber) {
		try {
			final String[][] columnExpressions = (String[][]) subclassPropertyFormulasMethod.invoke( persister );
			return columnExpressions[subclassPropertyNumber];
		}
		catch (InvocationTargetException e) {
			throw new HibernateException(
					"Unable to access AbstractEntityPersister#getSubclassPropertyTableNumber [" + persister.toString() + "]",
					e.getTargetException()
			);
		}
		catch (Exception e) {
			throw new HibernateException(
					"Unable to access AbstractEntityPersister#getSubclassPropertyTableNumber [" + persister.toString() + "]",
					e
			);
		}
	}

	public static List<Column> makeValues(
			SessionFactoryImplementor factory,
			Type type,
			String[] columns,
			String[] formulas,
			Table table) {
		assert formulas == null || columns.length == formulas.length;

		final List<Column> values = new ArrayList<>();

		for ( int i = 0; i < columns.length; i++ ) {
			final int jdbcType = type.sqlTypes()[i];

			if ( columns[i] != null ) {
				values.add( table.makeColumn( columns[i], jdbcType ) );
			}
			else {
				if ( formulas == null ) {
					throw new IllegalStateException( "Column name was null and no formula information was supplied" );
				}
				values.add( table.makeFormula( formulas[i], jdbcType ) );
			}
		}

		return values;
	}

	public Attribute buildAttribute(
			PersisterCreationContext creationContext,
			ManagedTypeImplementor source,
			Property property,
			List<Column> columns) {
		if ( property.getValue() instanceof Collection ) {
			assert columns == null || columns.isEmpty();

			return buildPluralAttribute(
					creationContext,
					source,
					property
			);
		}
		else {
			return buildSingularAttribute(
					creationContext,
					source,
					property,
					columns
			);
		}
	}

	public AbstractAttribute buildSingularAttribute(
			PersisterCreationContext creationContext,
			ManagedTypeImplementor source,
			Property property,
			List<Column> columns) {
		if ( property.getValue() instanceof Any ) {
			throw new NotYetImplementedException();
		}
		else if ( property.getValue() instanceof Component ) {
			return new SingularAttributeEmbedded(
					source,
					property.getName(),
					resolvePropertyAccess( creationContext, property ),
					Disposition.NORMAL,
					creationContext.getPersisterFactory().createEmbeddablePersister(
							(Component) property.getValue(),
							source,
							property.getName(),
							creationContext
					)
			);
		}
		else if ( property.getValue() instanceof ToOne ) {
			final ToOne toOne = (ToOne) property.getValue();

			if ( property.getValue() instanceof OneToOne ) {
				// the Classification here should be ONE_TO_ONE which could represent either a real PK one-to-one
				//		or a unique-FK one-to-one (logical).  If this is a real one-to-one then we should have
				//		no columns passed here and should instead use the LHS (source) PK column(s)
				assert columns == null || columns.size() == 0;
				columns = ( (EntityPersister) source ).getHierarchy().getIdentifierDescriptor().getColumns();
			}
			assert columns != null && columns.size() > 0;

			return new SingularAttributeEntity(
					source,
					property.getName(),
					resolvePropertyAccess( creationContext, property ),
					property.getValue() instanceof OneToOne || ( (ManyToOne) property.getValue() ).isLogicalOneToOne()
							? SingularAttributeClassification.ONE_TO_ONE
							: SingularAttributeClassification.MANY_TO_ONE,
					makeEntityType( creationContext, toOne ),
					Disposition.NORMAL,
					creationContext.getTypeConfiguration().findEntityPersister( toOne.getReferencedEntityName() ),
					columns
			);
		}
		else {
			assert property.getValue() instanceof SimpleValue;

			final SimpleValue simpleValue = (SimpleValue) property.getValue();

			final AttributeConverterDefinition attributeConverterInfo = simpleValue.getAttributeConverterDescriptor();

			return new SingularAttributeBasic<>(
					source,
					property.getName(),
					resolvePropertyAccess( creationContext, property ),
					resolveBasicType( creationContext, simpleValue ),
					Disposition.NORMAL,
					attributeConverterInfo,
					columns
			);

		}




		final SingularAttributeClassification classification = interpretSingularAttributeClassification( attributeType );
		if ( classification == SingularAttributeClassification.ANY ) {

		}
		else if ( classification == SingularAttributeClassification.EMBEDDED ) {
		}
		else if ( classification == SingularAttributeClassification.BASIC ) {
			// todo : need to be able to locate the AttributeConverter (if one) associated with this singular basic attribute
		}
		else {
		}
	}

	@SuppressWarnings("unchecked")
	private <J> BasicType<J> resolveBasicType(PersisterCreationContext creationContext, SimpleValue simpleValue) {
		if ( simpleValue.getCurrentType() != null ) {
			return (BasicType<J>) simpleValue.getCurrentType();
		}

		return creationContext.getTypeConfiguration().getBasicTypeRegistry().resolveBasicType(
				simpleValue.getBasicTypeParameters(),
				simpleValue.makeJdbcRecommendedSqlTypeMappingContext( creationContext.getTypeConfiguration() )
		);
	}

	private EntityType makeEntityType(PersisterCreationContext creationContext, ToOne toOne) {
		return new EntityTypeImpl(
				null,
		);
	}

	private PropertyAccess resolvePropertyAccess(PersisterCreationContext persisterCreationContext, Property property) {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	private static String extractEmbeddableName(Type attributeType) {
		// todo : fixme
		return attributeType.getName();
	}

	public Attribute buildPluralAttribute(
			PersisterCreationContext creationContext,
			Collection collectionBinding,
			AttributeContainer source,
			String propertyName,
			Type attributeType) {
		final CollectionType collectionType = (CollectionType) attributeType;
		final PersisterFactory persisterFactory = creationContext.getSessionFactory().getServiceRegistry().getService( PersisterFactory.class );

		// todo : resolve cache access
		final CollectionRegionAccessStrategy cachingAccess = null;

		// need PersisterCreationContext - we should always have access to that when building persisters, through finalized initialization
		final CollectionPersister collectionPersister = persisterFactory.createCollectionPersister(
				collectionBinding,
				source,
				propertyName,
				cachingAccess,
				creationContext
		);
		creationContext.registerCollectionPersister( collectionPersister );
		return collectionPersister;
	}

	public static org.hibernate.loader.PropertyPath convert(PropertyPath propertyPath) {
		if ( propertyPath.getParent() == null ) {
			return new org.hibernate.loader.PropertyPath( null, propertyPath.getLocalPath() );
		}
		org.hibernate.loader.PropertyPath parent = convert( propertyPath.getParent() );
		return parent.append( propertyPath.getLocalPath() );
	}

	public static interface CollectionMetadata {
		CollectionClassification getCollectionClassification();
		ElementClassification getElementClassification();

		Type getForeignKeyType();
		BasicType getCollectionIdType();
		Type getElementType();
		Type getIndexType();
	}

	public static class CollectionMetadataImpl implements CollectionMetadata {
		private final CollectionClassification collectionClassification;
		private final ElementClassification elementClassification;
		private final Type foreignKeyType;
		private final BasicType collectionIdType;
		private final Type elementType;
		private final Type indexType;

		public CollectionMetadataImpl(
				CollectionClassification collectionClassification,
				ElementClassification elementClassification,
				Type foreignKeyType,
				BasicType collectionIdType,
				Type elementType,
				Type indexType) {
			this.collectionClassification = collectionClassification;
			this.elementClassification = elementClassification;
			this.foreignKeyType = foreignKeyType;
			this.collectionIdType = collectionIdType;
			this.elementType = elementType;
			this.indexType = indexType;
		}

		@Override
		public CollectionClassification getCollectionClassification() {
			return collectionClassification;
		}

		@Override
		public ElementClassification getElementClassification() {
			return elementClassification;
		}

		@Override
		public Type getForeignKeyType() {
			return foreignKeyType;
		}

		@Override
		public BasicType getCollectionIdType() {
			return collectionIdType;
		}

		@Override
		public Type getElementType() {
			return elementType;
		}

		@Override
		public Type getIndexType() {
			return indexType;
		}
	}

	public static CollectionMetadata interpretCollectionMetadata(SessionFactoryImplementor factory, CollectionType collectionType) {
		final CollectionPersister collectionPersister = factory.getMetamodel().collectionPersister( collectionType.getRole() );

		return new CollectionMetadataImpl(
				interpretCollectionClassification( collectionType ),
				interpretElementClassification( collectionPersister ),
				collectionPersister.getKeyType(),
				(BasicType) collectionPersister.getIdentifierType(),
				collectionPersister.getElementType(),
				collectionPersister.getIndexType()
		);
	}

	public static CollectionClassification interpretCollectionClassification(CollectionType collectionType) {
		if ( collectionType instanceof BagType
				|| collectionType instanceof IdentifierBagType ) {
			return CollectionClassification.BAG;
		}
		else if ( collectionType instanceof ListType
				|| collectionType instanceof ArrayType ) {
			return CollectionClassification.LIST;
		}
		else if ( collectionType instanceof SetType
				|| collectionType instanceof OrderedSetType
				|| collectionType instanceof SortedSetType ) {
			return CollectionClassification.SET;
		}
		else if ( collectionType instanceof MapType
				|| collectionType instanceof OrderedMapType
				|| collectionType instanceof SortedMapType ) {
			return CollectionClassification.MAP;
		}
		else {
			final Class javaType = collectionType.getReturnedClass();
			if ( Set.class.isAssignableFrom( javaType ) ) {
				return CollectionClassification.SET;
			}
			else if ( Map.class.isAssignableFrom( javaType ) ) {
				return CollectionClassification.MAP;
			}
			else if ( List.class.isAssignableFrom( javaType ) ) {
				return CollectionClassification.LIST;
			}

			return CollectionClassification.BAG;
		}
	}

	private static ElementClassification interpretElementClassification(CollectionPersister collectionPersister) {
		final Type elementType = collectionPersister.getElementType();

		if ( elementType.isAnyType() ) {
			return ElementClassification.ANY;
		}
		else if ( elementType.isComponentType() ) {
			return ElementClassification.EMBEDDABLE;
		}
		else if ( elementType.isEntityType() ) {
			if ( collectionPersister.isManyToMany() ) {
				return ElementClassification.MANY_TO_MANY;
			}
			else {
				return ElementClassification.ONE_TO_MANY;
			}
		}
		else {
			return ElementClassification.BASIC;
		}
	}


	public static SingularAttributeClassification interpretIdentifierClassification(Type ormIdType) {
		return ormIdType instanceof EmbeddedType
				? SingularAttributeClassification.EMBEDDED
				: SingularAttributeClassification.BASIC;
	}
}
