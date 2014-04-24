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
package org.hibernate.metamodel.spi.binding;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.hibernate.AnnotationException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.CompositeNestedGeneratedValueGenerator;
import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.Schema;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tuple.component.ComponentMetamodel;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;

import static org.hibernate.id.EntityIdentifierNature.AGGREGATED_COMPOSITE;
import static org.hibernate.id.EntityIdentifierNature.NON_AGGREGATED_COMPOSITE;
import static org.hibernate.id.EntityIdentifierNature.SIMPLE;

/**
 * Hold information about the entity identifier.  At a high-level, can be one of
 * 2 types:<ul>
 *     <li>
 *         single-attribute identifier - this includes both simple identifiers
 *         and aggregated composite identifiers
 *     </li>
 *     <li>
 *         multi-attribute identifier - non-aggregated composite identifiers
 *     </li>
 * </ul>
 * <p/>
 * The EntityIdentifier instance is created when the entity hierarchy is, but at
 * that time we do not know all the information needed for interpreting the nature
 * of the identifier.  So at some point after creation, one of the prepare methods
 * must be called:<ul>
 *     <li>{@link #prepareAsSimpleIdentifier}</li>
 *     <li>{@link #prepareAsAggregatedCompositeIdentifier}</li>
 *     <li>{@link #prepareAsNonAggregatedCompositeIdentifier}</li>
 * </ul>
 * Attempts to prepare the EntityIdentifier more than once will result in an
 * exception.  Attempts to access the information contained in the
 * EntityIdentifier prior to binding will (generally) result in an
 * exception.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 * @author Gail Badner
 */
public class EntityIdentifier {
	private final EntityBinding entityBinding;

	private BindingImplementor identifierBinding;

	private IdentifierGenerator identifierGenerator;

	/**
	 * Create an identifier
	 *
	 * @param entityBinding the entity binding for which this instance is the id
	 */
	public EntityIdentifier(EntityBinding entityBinding) {
		this.entityBinding = entityBinding;
	}

	public EntityBinding getEntityBinding() {
		return entityBinding;
	}

	public SimpleIdentifierBinding prepareAsSimpleIdentifier(
			SingularAttributeBinding attributeBinding,
			IdentifierGeneratorDefinition identifierGeneratorDefinition,
			String unsavedValue) {
		ensureNotBound();
		this.identifierBinding = new SimpleIdentifierBindingImpl(
				this.getEntityBinding(),
				attributeBinding,
				identifierGeneratorDefinition,
				unsavedValue
		);
		return (SimpleIdentifierBinding) identifierBinding;
	}

	public AggregatedCompositeIdentifierBinding prepareAsAggregatedCompositeIdentifier(
			EmbeddedAttributeBinding attributeBinding,
			IdentifierGeneratorDefinition identifierGeneratorDefinition,
			String unsavedValue) {
		ensureNotBound();
		this.identifierBinding = new AggregatedCompositeIdentifierBindingImpl(
				this.getEntityBinding(),
				attributeBinding,
				identifierGeneratorDefinition,
				unsavedValue
		);
		return (AggregatedCompositeIdentifierBinding) identifierBinding;
	}

	public NonAggregatedCompositeIdentifierBinding prepareAsNonAggregatedCompositeIdentifier(
			EmbeddableBindingImplementor virtualEmbeddableBinding,
			EmbeddedAttributeBinding virtualAttributeBinding,
			EmbeddableBindingImplementor idClassEmbeddableBinding,
			IdentifierGeneratorDefinition identifierGeneratorDefinition,
			String unsavedValue) {
		ensureNotBound();
		this.identifierBinding = new NonAggregatedCompositeIdentifierBindingImpl(
				this.getEntityBinding(),
				virtualEmbeddableBinding,
				virtualAttributeBinding,
				idClassEmbeddableBinding,
				identifierGeneratorDefinition,
				unsavedValue
		);

		return (NonAggregatedCompositeIdentifierBinding) identifierBinding;
	}


	public EntityIdentifierNature getNature() {
		ensureBound();
		return identifierBinding.getNature();
	}

	public Binding getEntityIdentifierBinding() {
		ensureBound();
		return identifierBinding;
	}

	public boolean definesIdClass() {
		ensureBound();
		return getNature() == NON_AGGREGATED_COMPOSITE
				&& ( (NonAggregatedCompositeIdentifierBinding) identifierBinding ).getIdClassMetadata() != null;
	}

//	public IdClassMetadata getIdClassMetadata() {
//		return idClassMetadata;
//	}
//
//
//	/**
//	 * @deprecated  Use the {@link #getEntityIdentifierBinding} instead
//	 */
//	@Deprecated
//	public SingularAttributeBinding getAttributeBinding() {
//		ensureBound();
//		return identifierBinding.getAttributeBinding();
//	}
//
//	/**
//	 * @deprecated  Use the {@link #getEntityIdentifierBinding} instead
//	 */
//	@Deprecated
//	public boolean isIdentifierAttributeBinding(AttributeBinding attributeBinding) {
//		ensureBound();
//		return identifierBinding.isIdentifierAttributeBinding( attributeBinding );
//	}
//
//	/**
//	 * @deprecated No real replacement; not even used atm
//	 */
//	@Deprecated
//	public boolean isCascadeDeleteEnabled() {
//		if ( getAttributeBinding() instanceof Cascadeable ) {
//			Cascadeable cascadeable = Cascadeable.class.cast( getAttributeBinding() );
//			cascadeable.getCascadeStyle();//todo
//		}
//		return false;
//	}
//
//	/**
//	 * @deprecated  Use the {@link #getEntityIdentifierBinding} instead
//	 */
//	@Deprecated
//	public String getUnsavedValue() {
//		ensureBound();
//		return identifierBinding.getUnsavedValue();
//	}
//
//	/**
//	 * @deprecated  Check nature
//	 */
//	@Deprecated
//	public boolean isNonAggregatedComposite() {
//		ensureBound();
//		return getNature() == EntityIdentifierNature.NON_AGGREGATED_COMPOSITE;
//	}
//
//	/**
//	 * Get the Class of the {@link javax.persistence.IdClass} associated with the entity, if one.
//	 *
//	 * @deprecated Use {@link #getIdClassMetadata()} instead
//	 */
//	@Deprecated
//	public JavaTypeDescriptor getIdClassClass() {
//		ensureBound();
//		return getIdClassMetadata().getIdClassType();
//	}
//
//	/**
//	 * @deprecated Use {@link #getIdClassMetadata()} instead
//	 */
//	@Deprecated
//	public String getIdClassPropertyAccessorName() {
//		ensureBound();
//		return null;
//	}
//
//	/**
//	 * @deprecated Use {@link #getIdClassMetadata()} instead
//	 */
//	@Deprecated
//	public boolean isIdentifierMapper() {
//		ensureBound();
//		return isNonAggregatedComposite() && getIdClassMetadata().getIdClassType() != null;
//	}

	// todo do we really need this createIdentifierGenerator and how do we make sure the getter is not called too early
	// maybe some sort of visitor pattern here!? (HF)
	public IdentifierGenerator createIdentifierGenerator(IdentifierGeneratorFactory factory, Properties properties) {
		ensureBound();
		if ( identifierGenerator == null ) {
			identifierGenerator = identifierBinding.createIdentifierGenerator( factory, properties );
		}
		return identifierGenerator;
	}

	public IdentifierGenerator getIdentifierGenerator() {
		ensureBound();
		return identifierGenerator;
	}

	protected void ensureBound() {
		if ( ! isBound() ) {
			throw new IllegalStateException( "Entity identifier was not yet bound" );
		}
	}

	protected void ensureNotBound() {
		if ( isBound() ) {
			throw new IllegalStateException( "Entity identifier was already bound" );
		}
	}

	public boolean isBound() {
		return identifierBinding != null;
	}


	/**
	 * @deprecated Use {@link #getEntityIdentifierBinding()} instead
	 */
	@Deprecated
	public int getColumnCount() {
		ensureBound();
		return identifierBinding.getColumnCount();
	}

	// todo : expose IdClassMetadata just from NonAggregatedCompositeIdentifierBindingImpl ?
	//		- that is the only place it has relevance

	/**
	 * Models {@link javax.persistence.IdClass} information.
	 */
	public static interface IdClassMetadata {
		public EmbeddableBinding getEmbeddableBinding();
		public JavaTypeDescriptor getIdClassType();
		public String getAccessStrategy(String attributeName);
		public Type getHibernateType(ServiceRegistry serviceRegistry, TypeFactory typeFactory);
	}

	private static class IdClassMetadataImpl implements IdClassMetadata {
		private final EmbeddableBinding idClassBinding;

		private Type type;

		private IdClassMetadataImpl(EmbeddableBinding idClassBinding) {
			this.idClassBinding = idClassBinding;
		}

		@Override
		public EmbeddableBinding getEmbeddableBinding() {
			return idClassBinding;
		}

		@Override
		public JavaTypeDescriptor getIdClassType() {
			return idClassBinding.getTypeDescriptor();
		}

		@Override
		public String getAccessStrategy(String attributeName) {
			final AttributeBinding attributeBinding = idClassBinding.locateAttributeBinding( attributeName );
			if ( attributeBinding == null ) {
				throw new AnnotationException(
						String.format(
								Locale.ENGLISH,
								"Unable to locate IdClass attribute by that name : %s, %s",
								getIdClassType().getName().toString(),
								attributeName
						)
				);
			}

			return attributeBinding.getPropertyAccessorName();
		}

		@Override
		public Type getHibernateType(ServiceRegistry serviceRegistry, TypeFactory typeFactory) {
			if ( type == null && idClassBinding != null ) {
				type = typeFactory.embeddedComponent(
						new ComponentMetamodel( serviceRegistry, idClassBinding, true, false )
				);
			}
			return type;
		}
	}

	public static interface Binding {
		public EntityIdentifierNature getNature();
		public SingularAttributeBinding getAttributeBinding();
		public String getUnsavedValue();
		public List<RelationalValueBinding> getRelationalValueBindings();
		public boolean isIdentifierAttributeBinding(AttributeBinding attributeBinding);
		public Type getHibernateType();
	}

	public static interface AttributeBasedIdentifierBinding extends Binding {
	}

	public static interface SimpleIdentifierBinding extends AttributeBasedIdentifierBinding {
	}

	public static interface AggregatedCompositeIdentifierBinding extends AttributeBasedIdentifierBinding {
		@Override
		public EmbeddedAttributeBinding getAttributeBinding();
	}

	public static interface NonAggregatedCompositeIdentifierBinding extends Binding {
		/**
		 * Obtain the virtual Embeddable representation of the identifier.  This
		 * maps to the entity class, but handles just the id attributes.
		 *
		 * @return The virtual Embeddable binding
		 */
		public EmbeddableBinding getVirtualEmbeddableBinding();

		/**
		 * Obtain metadata about the IdClass, if one.
		 *
		 * @return Metadata about the defined IdClass, or {@code null} if
		 * no IdClass was defined.
		 */
		public IdClassMetadata getIdClassMetadata();

		/**
		 * Builds the Type for the "virtual embeddable".  See
		 * {@link IdClassMetadata#getHibernateType} for obtaining the Type
		 * relating to the IdClass
		 *
		 * Caches the Type reference after first call.
		 *
		 * @param serviceRegistry The ServiceRegistry
		 * @param typeFactory The TypeFactory
		 *
		 * @return The resolved Type
		 */
		public Type getHibernateType(ServiceRegistry serviceRegistry, TypeFactory typeFactory);
	}

	private static interface BindingImplementor extends Binding {
		public int getColumnCount();
		public IdentifierGenerator createIdentifierGenerator(
				IdentifierGeneratorFactory identifierGeneratorFactory,
				Properties properties);
	}

	private static abstract class AbstractIdentifierBinding implements BindingImplementor {
		private final EntityBinding entityBinding;
		private final IdentifierGeneratorDefinition identifierGeneratorDefinition;
		private final String unsavedValue;

		protected AbstractIdentifierBinding(
				EntityBinding entityBinding,
				IdentifierGeneratorDefinition identifierGeneratorDefinition,
				String unsavedValue) {
			this.entityBinding = entityBinding;
			this.identifierGeneratorDefinition = identifierGeneratorDefinition;
			this.unsavedValue = unsavedValue;
		}

		public EntityBinding getEntityBinding() {
			return entityBinding;
		}

		@Override
		public String getUnsavedValue() {
			return unsavedValue;
		}

		public IdentifierGeneratorDefinition getGeneratorDefinition() {
			return identifierGeneratorDefinition;
		}
	}

	private static abstract class AbstractAttributeBasedIdentifierBinding
			extends AbstractIdentifierBinding
			implements AttributeBasedIdentifierBinding {
		private final SingularAttributeBinding identifierAttributeBinding;
		private final int columnCount;

		protected AbstractAttributeBasedIdentifierBinding(
				EntityBinding entityBinding,
				SingularAttributeBinding identifierAttributeBinding,
				IdentifierGeneratorDefinition identifierGeneratorDefinition,
				String unsavedValue) {
			super( entityBinding, identifierGeneratorDefinition, unsavedValue );
			this.identifierAttributeBinding = identifierAttributeBinding;

			// Configure primary key in relational model
			final List<RelationalValueBinding> relationalValueBindings = identifierAttributeBinding.getRelationalValueBindings();
			this.columnCount = relationalValueBindings.size();
			for ( final RelationalValueBinding valueBinding : relationalValueBindings ) {
				entityBinding.getPrimaryTable()
						.getPrimaryKey()
						.addColumn( (Column) valueBinding.getValue() );
			}
		}

		@Override
		public SingularAttributeBinding getAttributeBinding() {
			return identifierAttributeBinding;
		}

		@Override
		public int getColumnCount() {
			return columnCount;
		}

		@Override
		public List<RelationalValueBinding> getRelationalValueBindings() {
			return getAttributeBinding().getRelationalValueBindings();
		}

		@Override
		public boolean isIdentifierAttributeBinding(AttributeBinding attributeBinding) {
			return getAttributeBinding().equals( attributeBinding );
		}

		@Override
		public IdentifierGenerator createIdentifierGenerator(
				IdentifierGeneratorFactory identifierGeneratorFactory,
				Properties properties) {

			final List<RelationalValueBinding> valueBindings = getAttributeBinding().getRelationalValueBindings();
			final TableSpecification table = getEntityBinding().getPrimaryTable();

			Properties params = new Properties();
			params.putAll( properties );

			// use the schema/catalog specified by getValue().getTable() - but note that
			// if the schema/catalog were specified as params, they will already be initialized and
			//will override the values set here (they are in identifierGeneratorDefinition.getParameters().)
			Schema schema = table.getSchema();
			if ( schema != null ) {
				if ( schema.getName().getSchema() != null ) {
					params.setProperty( PersistentIdentifierGenerator.SCHEMA, schema.getName().getSchema().getText() );
				}
				if ( schema.getName().getCatalog() != null ) {
					params.setProperty( PersistentIdentifierGenerator.CATALOG, schema.getName().getCatalog().getText() );
				}
			}

			params.setProperty( IdentifierGenerator.ENTITY_NAME, getEntityBinding().getEntityName() );
			params.setProperty( IdentifierGenerator.JPA_ENTITY_NAME, getEntityBinding().getJpaEntityName() );

			//init the table here instead of earlier, so that we can get a quoted table name
			//TODO: would it be better to simply pass the qualified table name, instead of
			//      splitting it up into schema/catalog/table names
			String tableName = table.getQualifiedName( identifierGeneratorFactory.getDialect() );
			params.setProperty( PersistentIdentifierGenerator.TABLE, tableName );

			if ( valueBindings.size() == 1 ) {
				final RelationalValueBinding rvBinding = valueBindings.get( 0 );
				if ( Column.class.isInstance( rvBinding.getValue() ) ) {
					final Column pkColumn = (Column) rvBinding.getValue();
					params.setProperty(
							PersistentIdentifierGenerator.PK,
							pkColumn.getColumnName().getText( identifierGeneratorFactory.getDialect() )
					);
				}
			}

			if ( getEntityBinding().getHierarchyDetails().getInheritanceType() != InheritanceType.TABLE_PER_CLASS ) {
				params.setProperty( PersistentIdentifierGenerator.TABLES, tableName );
			}
			else {
				params.setProperty(
						PersistentIdentifierGenerator.TABLES,
						resolveTableNames( identifierGeneratorFactory.getDialect(), getEntityBinding() )
				);
			}
			params.putAll( getGeneratorDefinition().getParameters() );
			return identifierGeneratorFactory.createIdentifierGenerator(
					getGeneratorDefinition().getStrategy(),
					getAttributeBinding().getHibernateTypeDescriptor().getResolvedTypeMapping(),
					params
			);
		}

		private String resolveTableNames(Dialect dialect, EntityBinding entityBinding) {
			EntityBinding[] ebs = entityBinding.getPostOrderSubEntityBindingClosure();
			StringBuilder tableNames = new StringBuilder();
			String tbName = resolveTableName( dialect, entityBinding );
			if( StringHelper.isNotEmpty( tbName )){
				tableNames.append( tbName );
			}

			for ( EntityBinding eb : ebs ) {
				tbName = resolveTableName( dialect, eb );
				if(StringHelper.isNotEmpty( tbName )){
					tableNames.append( ", " ).append( tbName );
				}
			}
			return tableNames.toString();
		}

		private String resolveTableName(Dialect dialect, EntityBinding entityBinding) {
			TableSpecification tableSpecification = entityBinding.getPrimaryTable();
			if ( tableSpecification instanceof Table ) {
				Table tb = (Table) tableSpecification;
				if ( tb.isPhysicalTable() ) {
					return tb.getTableName().toText( dialect );
				}
			}
			return null;
		}

		@Override
		public Type getHibernateType() {
			return getAttributeBinding().getHibernateTypeDescriptor().getResolvedTypeMapping();
		}
	}

	private class SimpleIdentifierBindingImpl
			extends AbstractAttributeBasedIdentifierBinding
			implements SimpleIdentifierBinding {
		SimpleIdentifierBindingImpl(
				EntityBinding entityBinding,
				SingularAttributeBinding identifierAttributeBinding,
				IdentifierGeneratorDefinition identifierGeneratorDefinition,
				String unsavedValue) {
			super( entityBinding, identifierAttributeBinding, identifierGeneratorDefinition, unsavedValue );
		}

		@Override
		public EntityIdentifierNature getNature() {
			return SIMPLE;
		}
	}

	private class AggregatedCompositeIdentifierBindingImpl
			extends AbstractAttributeBasedIdentifierBinding
			implements AggregatedCompositeIdentifierBinding {
		AggregatedCompositeIdentifierBindingImpl(
				EntityBinding entityBinding,
				EmbeddedAttributeBinding identifierAttributeBinding,
				IdentifierGeneratorDefinition identifierGeneratorDefinition,
				String unsavedValue) {
			super( entityBinding, identifierAttributeBinding, identifierGeneratorDefinition, unsavedValue );
			if ( ! identifierAttributeBinding.getEmbeddableBinding().isAggregated() ) {
				throw new IllegalArgumentException(
						String.format(
								"identifierAttributeBinding must be an aggregated EmbeddedAttributeBinding: %s",
								identifierAttributeBinding.getAttribute().getName()
						)
				);
			}
		}

		@Override
		public EntityIdentifierNature getNature() {
			return AGGREGATED_COMPOSITE;
		}

		@Override
		public EmbeddedAttributeBinding getAttributeBinding() {
			return (EmbeddedAttributeBinding) super.getAttributeBinding();
		}

		@Override
		public IdentifierGenerator createIdentifierGenerator(
				IdentifierGeneratorFactory factory,
				Properties properties) {
			if ( entityBinding.getSuperEntityBinding() != null ) {
				throw new AssertionError( "Creating an identifier generator for a component on a subclass." );
			}

			final boolean hasCustomGenerator = ! "assigned".equals( getGeneratorDefinition().getStrategy() );
			if ( hasCustomGenerator ) {
				return super.createIdentifierGenerator(
						factory, properties
				);
			}

			// IMPL NOTE : See the javadoc discussion on CompositeNestedGeneratedValueGenerator wrt the
			//		various scenarios for which we need to account here
			// we have the "@EmbeddedId" / <composite-id name="idName"/> case
			CompositeNestedGeneratedValueGenerator.GenerationContextLocator locator =
					new CompositeNestedGeneratedValueGenerator.GenerationContextLocator() {
						public Serializable locateGenerationContext( SessionImplementor session, Object incomingObject) {
							return session.getEntityPersister( entityBinding.getEntityName(), incomingObject )
									.getIdentifier( incomingObject, session );
						}
					};
			// TODO: set up IdentifierGenerator for non-assigned sub-attributes
			return new CompositeNestedGeneratedValueGenerator( locator );
		}
	}

	private class NonAggregatedCompositeIdentifierBindingImpl
			extends AbstractIdentifierBinding
			implements NonAggregatedCompositeIdentifierBinding {
		private final EmbeddableBindingImplementor virtualEmbeddableBinding;
		private final EmbeddedAttributeBinding virtualAttributeBinding;

		private final IdClassMetadata idClassMetadata;

		private Type type;

		public NonAggregatedCompositeIdentifierBindingImpl(
				EntityBinding entityBinding,
				EmbeddableBindingImplementor virtualEmbeddableBinding,
				EmbeddedAttributeBinding virtualAttributeBinding,
				EmbeddableBindingImplementor idClassEmbeddableBinding,
				IdentifierGeneratorDefinition identifierGeneratorDefinition,
				String unsavedValue) {
			super( entityBinding, identifierGeneratorDefinition, unsavedValue );
			this.virtualEmbeddableBinding = virtualEmbeddableBinding;
			this.virtualAttributeBinding = virtualAttributeBinding;
			this.idClassMetadata = idClassEmbeddableBinding == null
					? null
					: new IdClassMetadataImpl( idClassEmbeddableBinding );

			final List<RelationalValueBinding> relationalValueBindings = virtualAttributeBinding.getRelationalValueBindings();
			for ( final RelationalValueBinding valueBinding : relationalValueBindings ) {
				entityBinding.getPrimaryTable()
						.getPrimaryKey()
						.addColumn( (Column) valueBinding.getValue() );
			}
		}

		@Override
		public EntityIdentifierNature getNature() {
			return NON_AGGREGATED_COMPOSITE;
		}

		@Override
		public EmbeddableBinding getVirtualEmbeddableBinding() {
			return virtualEmbeddableBinding;
		}

		@Override
		public SingularAttributeBinding getAttributeBinding() {
			return virtualAttributeBinding;
		}

		@Override
		public IdClassMetadata getIdClassMetadata() {
			return idClassMetadata;
		}

		@Override
		public List<RelationalValueBinding> getRelationalValueBindings() {
			return virtualAttributeBinding.getRelationalValueBindings();
		}

		@Override
		public int getColumnCount() {
			return getRelationalValueBindings().size();
		}

		@Override
		public boolean isIdentifierAttributeBinding(AttributeBinding attributeBinding) {
			if ( virtualAttributeBinding.equals( attributeBinding ) ) {
				return true;

			}
			for ( AttributeBinding embAttrBinding : virtualEmbeddableBinding.attributeBindings() ) {
				if ( embAttrBinding.equals( attributeBinding ) ) {
					return true;
				}
			}
			return false;
		}

		@Override
		public IdentifierGenerator createIdentifierGenerator(
				IdentifierGeneratorFactory factory,
				Properties properties) {
			if ( entityBinding.getSuperEntityBinding() != null ) {
				throw new AssertionError( "Creating an identifier generator for a component on a subclass." );
			}
			// IMPL NOTE : See the javadoc discussion on CompositeNestedGeneratedValueGenerator wrt the
			//		various scenarios for which we need to account here
			//if ( idClassClass != null ) {
			// we have the @IdClass / <composite-id mapped="true"/> case
			//	attributeDeclarer = idClassClass;
			//}
			//else {
			// we have the "straight up" embedded (again the hibernate term) component identifier
			//	attributeDeclarer = entityClass;
			//}
			CompositeNestedGeneratedValueGenerator.GenerationContextLocator locator =
					new CompositeNestedGeneratedValueGenerator.GenerationContextLocator() {
						public Serializable locateGenerationContext( SessionImplementor session, Object incomingObject) {
							return session.getEntityPersister( entityBinding.getEntityName(), incomingObject )
									.getIdentifier( incomingObject, session );
						}
					};
			// TODO: set up IdentifierGenerator for non-assigned sub-attributes
			return new CompositeNestedGeneratedValueGenerator( locator );
		}

		@Override
		public Type getHibernateType(ServiceRegistry serviceRegistry, TypeFactory typeFactory) {
			if ( type == null ) {
				type = typeFactory.embeddedComponent(
						new ComponentMetamodel(
								serviceRegistry,
								virtualEmbeddableBinding,
								true,
								false
						)
				);
			}
			return type;
		}

		@Override
		public Type getHibernateType() {
			return type;
		}
	}



//	private static class ValueGenerationPlan implements CompositeNestedGeneratedValueGenerator.GenerationPlan {
//		private final String propertyName;
//		private final IdentifierGenerator subGenerator;
//		private final Setter injector;
//
//		public ValueGenerationPlan(
//				String propertyName,
//				IdentifierGenerator subGenerator,
//				Setter injector) {
//			this.propertyName = propertyName;
//			this.subGenerator = subGenerator;
//			this.injector = injector;
//		}
//
//		/**
//		 * {@inheritDoc}
//		 */
//		public void execute(SessionImplementor session, Object incomingObject, Object injectionContext) {
//			final Object generatedValue = subGenerator.generate( session, incomingObject );
//			injector.set( injectionContext, generatedValue, session.getFactory() );
//		}
//
//		public void registerPersistentGenerators(Map generatorMap) {
//			if ( PersistentIdentifierGenerator.class.isInstance( subGenerator ) ) {
//				generatorMap.put( ( (PersistentIdentifierGenerator) subGenerator ).generatorKey(), subGenerator );
//			}
//		}
//	}
}
