/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.common.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.internal.ImprovedCollectionPersisterImpl;
import org.hibernate.persister.common.spi.AbstractAttributeImpl;
import org.hibernate.persister.common.spi.AbstractTable;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.embeddable.spi.EmbeddablePersister;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.persister.entity.spi.ImprovedEntityPersister;
import org.hibernate.sql.sqm.convert.spi.NotYetImplementedException;
import org.hibernate.sqm.domain.ManagedType;
import org.hibernate.sqm.domain.PluralAttribute;
import org.hibernate.sqm.domain.PluralAttribute.CollectionClassification;
import org.hibernate.sqm.domain.SingularAttribute;
import org.hibernate.type.ArrayType;
import org.hibernate.type.BagType;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.CollectionType;
import org.hibernate.type.spi.ColumnMapping;
import org.hibernate.type.spi.EmbeddableType;
import org.hibernate.type.IdentifierBagType;
import org.hibernate.type.ListType;
import org.hibernate.type.MapType;
import org.hibernate.type.OrderedMapType;
import org.hibernate.type.OrderedSetType;
import org.hibernate.type.SetType;
import org.hibernate.type.SortedMapType;
import org.hibernate.type.SortedSetType;
import org.hibernate.type.spi.Type;

/**
 * For now mainly a helper for reflection into stuff not exposed on the entity/collection persister
 * contracts
 *
 * @author Steve Ebersole
 */
public class Helper {
	private final Method subclassTableSpanMethod;
	private final Method subclassPropertyTableNumberMethod;
	private final Method subclassPropertyColumnsMethod;
	private final Method subclassPropertyFormulasMethod;

	/**
	 * Singleton access
	 */
	public static final Helper INSTANCE = new Helper();

	private Helper() {
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

	public static Column[] makeValues(
			SessionFactoryImplementor factory,
			AbstractTable containingTable,
			Type type,
			String[] columns,
			String[] formulas) {
		assert formulas == null || columns.length == formulas.length;

		final Column[] values = new Column[columns.length];

		for ( int i = 0; i < columns.length; i++ ) {
			final ColumnMapping columnMapping = determineColumnMapping( type, i );

			if ( columns[i] != null ) {
				values[i] = containingTable.makeColumn( columns[i], columnMapping.getSqlTypeDescriptor().getSqlType() );
			}
			else {
				if ( formulas == null ) {
					throw new IllegalStateException( "Column name was null and no formula information was supplied" );
				}
				values[i] = containingTable.makeFormula( formulas[i], columnMapping.getSqlTypeDescriptor().getSqlType() );
			}
		}

		return values;
	}

	private static ColumnMapping determineColumnMapping(Type type, int i) {
		if ( type instanceof BasicType ) {
			if ( i > 0 ) {
				throw new IllegalArgumentException( "BasicType can be mapped to just one column" );
			}
			return ( (BasicType) type ).getColumnMapping();
		}

		throw new NotYetImplementedException(  );
	}

	public AbstractAttributeImpl buildAttribute(
			DatabaseModel databaseModel,
			DomainMetamodelImpl domainMetamodel,
			ManagedType source,
			String propertyName,
			Type propertyType,
			Column[] columns) {
		if ( propertyType.getClassification() == Type.Classification.COLLECTION ) {
			return buildPluralAttribute(
					databaseModel,
					domainMetamodel,
					source,
					propertyName,
					propertyType,
					columns
			);
		}
		else {
			return buildSingularAttribute(
					databaseModel,
					domainMetamodel,
					source,
					propertyName,
					propertyType,
					columns
			);
		}
	}

	public AbstractAttributeImpl buildSingularAttribute(
			DatabaseModel databaseModel,
			DomainMetamodelImpl domainMetamodel,
			ManagedType source,
			String attributeName,
			org.hibernate.type.Type attributeType,
			Column[] columns) {
		final SingularAttribute.Classification classification = interpretSingularAttributeClassification( attributeType );
		if ( classification == SingularAttribute.Classification.ANY ) {
			throw new NotYetImplementedException();
		}
		else if ( classification == SingularAttribute.Classification.EMBEDDED ) {
			return new SingularAttributeEmbedded(
					source,
					attributeName,
					buildEmbeddablePersister(
							databaseModel,
							domainMetamodel,
							source.getTypeName() + '.' + attributeName,
							(EmbeddableType) attributeType,
							columns
					)
			);
		}
		else if ( classification == SingularAttribute.Classification.BASIC ) {
			return new SingularAttributeBasic(
					source,
					attributeName,
					(org.hibernate.type.spi.BasicType) attributeType,
					columns
			);
		}
		else {
			final org.hibernate.type.EntityType ormEntityType = (org.hibernate.type.EntityType) attributeType;
			if ( ormEntityType.isOneToOne() ) {
				// the Classification here should be ONE_TO_ONE which could represent either a real PK one-to-one
				//		or a unique-FK one-to-one (logical).  If this is a real one-to-one then we should have
				//		no columns passed here and should instead use the LHS (source) PK column(s)
				assert columns == null || columns.length == 0;
				columns = ( (ImprovedEntityPersister) source ).getIdentifierDescriptor().getColumns();
			}
			assert columns != null && columns.length > 0;

			return new SingularAttributeEntity(
					source,
					attributeName,
					classification,
					ormEntityType,
					domainMetamodel.toSqmType( ormEntityType ),
					columns
			);
		}
	}

	public EmbeddablePersister buildEmbeddablePersister(
			DatabaseModel databaseModel,
			DomainMetamodelImpl domainMetamodel,
			String role,
			EmbeddableType embeddableType,
			Column[] columns) {
		return new EmbeddablePersister(
				extractEmbeddableName( embeddableType ),
				role,
				embeddableType,
				databaseModel,
				domainMetamodel,
				columns
		);
	}

	private static String extractEmbeddableName(org.hibernate.type.Type attributeType) {
		// todo : fixme
		return attributeType.getName();
	}

	public AbstractAttributeImpl buildPluralAttribute(
			DatabaseModel databaseModel,
			DomainMetamodelImpl domainMetamodel,
			ManagedType source,
			String subclassPropertyName,
			Type attributeType,
			Column[] columns) {
		final CollectionType collectionType = (CollectionType) attributeType;
		final CollectionPersister collectionPersister = domainMetamodel.getSessionFactory().getMetamodel().collectionPersister( collectionType.getRole() );

		final ImprovedCollectionPersisterImpl persister = new ImprovedCollectionPersisterImpl(
				source,
				subclassPropertyName,
				collectionPersister,
				columns
		);

		domainMetamodel.registerCollectionPersister( persister );
		return persister;
	}

	public static interface CollectionMetadata {
		CollectionClassification getCollectionClassification();
		PluralAttribute.ElementClassification getElementClassification();

		Type getForeignKeyType();
		BasicType getCollectionIdType();
		Type getElementType();
		Type getIndexType();
	}

	public static class CollectionMetadataImpl implements CollectionMetadata {
		private final CollectionClassification collectionClassification;
		private final PluralAttribute.ElementClassification elementClassification;
		private final Type foreignKeyType;
		private final BasicType collectionIdType;
		private final Type elementType;
		private final Type indexType;

		public CollectionMetadataImpl(
				CollectionClassification collectionClassification,
				PluralAttribute.ElementClassification elementClassification,
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
		public PluralAttribute.ElementClassification getElementClassification() {
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
		final CollectionPersister collectionPersister = factory.getCollectionPersister( collectionType.getRole() );

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

	private static PluralAttribute.ElementClassification interpretElementClassification(CollectionPersister collectionPersister) {
		final Type elementType = collectionPersister.getElementType();

		if ( elementType.isAnyType() ) {
			return PluralAttribute.ElementClassification.ANY;
		}
		else if ( elementType.isComponentType() ) {
			return PluralAttribute.ElementClassification.EMBEDDABLE;
		}
		else if ( elementType.isEntityType() ) {
			if ( collectionPersister.isManyToMany() ) {
				return PluralAttribute.ElementClassification.MANY_TO_MANY;
			}
			else {
				return PluralAttribute.ElementClassification.ONE_TO_MANY;
			}
		}
		else {
			return PluralAttribute.ElementClassification.BASIC;
		}
	}

	public static SingularAttribute.Classification interpretSingularAttributeClassification(Type attributeType) {
		assert !attributeType.isCollectionType();

		if ( attributeType.isAnyType() ) {
			return SingularAttribute.Classification.ANY;
		}
		else if ( attributeType.isEntityType() ) {
			final org.hibernate.type.EntityType ormEntityType = (org.hibernate.type.EntityType) attributeType;
			return ormEntityType.isOneToOne() || ormEntityType.isLogicalOneToOne()
					? SingularAttribute.Classification.ONE_TO_ONE
					: SingularAttribute.Classification.MANY_TO_ONE;
		}
		else if ( attributeType.isComponentType() ) {
			return SingularAttribute.Classification.EMBEDDED;
		}
		else {
			return SingularAttribute.Classification.BASIC;
		}
	}

	public static SingularAttribute.Classification interpretIdentifierClassification(Type ormIdType) {
		return ormIdType instanceof EmbeddableType
				? SingularAttribute.Classification.EMBEDDED
				: SingularAttribute.Classification.BASIC;
	}
}
