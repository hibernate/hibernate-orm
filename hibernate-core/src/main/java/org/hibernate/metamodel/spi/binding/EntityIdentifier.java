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
import java.util.Map;
import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.CompositeNestedGeneratedValueGenerator;
import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.Schema;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.property.Setter;

import static org.hibernate.id.EntityIdentifierNature.AGGREGATED_COMPOSITE;
import static org.hibernate.id.EntityIdentifierNature.NON_AGGREGATED_COMPOSITE;
import static org.hibernate.id.EntityIdentifierNature.SIMPLE;

/**
 * Hold information about the entity identifier.  At a high-level, can be one of 2-types:<ul>
 *     <li>single-attribute identifier - this includes both simple identifiers and aggregated composite identifiers</li>
 *     <li>multiple-attribute identifier - non-aggregated composite identifiers</li>
 * </ul>
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 * @author Gail Badner
 */
public class EntityIdentifier {
	private final EntityBinding entityBinding;
	private EntityIdentifierBinding entityIdentifierBinding;
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

	public void prepareAsSimpleIdentifier(
			SingularNonAssociationAttributeBinding attributeBinding,
			IdentifierGeneratorDefinition identifierGeneratorDefinition,
			String unsavedValue) {
		ensureNotBound();
		this.entityIdentifierBinding =
				new SimpleAttributeIdentifierBindingImpl( attributeBinding, identifierGeneratorDefinition, unsavedValue );
	}

	public void prepareAsAggregatedCompositeIdentifier(
			CompositeAttributeBinding attributeBinding,
			IdentifierGeneratorDefinition identifierGeneratorDefinition,
			String unsavedValue) {
		ensureNotBound();
		this.entityIdentifierBinding =
				new AggregatedComponentIdentifierBindingImpl( attributeBinding,
						identifierGeneratorDefinition, unsavedValue );
	}

	public void prepareAsNonAggregatedCompositeIdentifier(
			CompositeAttributeBinding compositeAttributeBinding,
			IdentifierGeneratorDefinition identifierGeneratorDefinition,
			String unsavedValue,
			Class<?> externalAggregatingClass,
			String externalAggregatingPropertyAccessorName) {
		ensureNotBound();
		this.entityIdentifierBinding = new NonAggregatedCompositeIdentifierBindingImpl(
				compositeAttributeBinding,
				identifierGeneratorDefinition,
				unsavedValue,
				externalAggregatingClass,
				externalAggregatingPropertyAccessorName
		);
	}

	public EntityIdentifierNature getNature() {
		ensureBound();
		return entityIdentifierBinding.getNature();
	}

	public SingularNonAssociationAttributeBinding getAttributeBinding() {
		ensureBound();
		return entityIdentifierBinding.getAttributeBinding();
	}

	public boolean isIdentifierAttributeBinding(AttributeBinding attributeBinding) {
		ensureBound();
		return entityIdentifierBinding.isIdentifierAttributeBinding( attributeBinding );
	}

	public boolean isCascadeDeleteEnabled() {
		if ( getAttributeBinding() instanceof Cascadeable ) {
			Cascadeable cascadeable = Cascadeable.class.cast( getAttributeBinding() );
			cascadeable.getCascadeStyle();//todo
		}
		return false;
	}

	public String getUnsavedValue() {
		ensureBound();
		return entityIdentifierBinding.getUnsavedValue();
	}

	public boolean isNonAggregatedComposite() {
		ensureBound();
		return getNature() == EntityIdentifierNature.NON_AGGREGATED_COMPOSITE;
	}


	public Class getIdClassClass() {
		ensureBound();
		ensureNonAggregatedComposite();
		return ( (NonAggregatedCompositeIdentifierBindingImpl) entityIdentifierBinding ).getIdClassClass();
	}

	public String getIdClassPropertyAccessorName() {
		ensureBound();
		ensureNonAggregatedComposite();
		return ( (NonAggregatedCompositeIdentifierBindingImpl) entityIdentifierBinding ).getIdClassPropertyAccessorName();
	}

	private void ensureNonAggregatedComposite() {
		if ( ! isNonAggregatedComposite() ) {
			throw new UnsupportedOperationException(
					String.format(
							"Entity identifiers of nature %s does not support idClasses.",
							entityIdentifierBinding.getNature()
					)
			);
		}
	}

	public boolean isIdentifierMapper() {
		ensureBound();
		return isNonAggregatedComposite() &&
				( (NonAggregatedCompositeIdentifierBindingImpl) entityIdentifierBinding ).getIdClassClass() != null;
	}

	// todo do we really need this createIdentifierGenerator and how do we make sure the getter is not called too early
	// maybe some sort of visitor pattern here!? (HF)
	public IdentifierGenerator createIdentifierGenerator(IdentifierGeneratorFactory factory, Properties properties) {
		ensureBound();
		if ( identifierGenerator == null ) {
			identifierGenerator = entityIdentifierBinding.createIdentifierGenerator( factory, properties );
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

	protected boolean isBound() {
		return entityIdentifierBinding != null;
	}

	public int getColumnCount() {
		ensureBound();
		return entityIdentifierBinding.getColumnCount();
	}

	private abstract class EntityIdentifierBinding {
		private final EntityIdentifierNature nature;
		private final SingularNonAssociationAttributeBinding identifierAttributeBinding;
		private final IdentifierGeneratorDefinition identifierGeneratorDefinition;
		private final String unsavedValue;
		private final int columnCount;


		protected EntityIdentifierBinding(
				EntityIdentifierNature nature,
				SingularNonAssociationAttributeBinding identifierAttributeBinding,
				IdentifierGeneratorDefinition identifierGeneratorDefinition,
				String unsavedValue) {
			this.nature = nature;
			this.identifierAttributeBinding = identifierAttributeBinding;
			this.identifierGeneratorDefinition = identifierGeneratorDefinition;
			this.unsavedValue = unsavedValue;

			// Configure primary key in relational model
			final List<RelationalValueBinding> relationalValueBindings = identifierAttributeBinding.getRelationalValueBindings();
			this.columnCount = relationalValueBindings.size();
			for ( final RelationalValueBinding valueBinding : relationalValueBindings ) {
				entityBinding.getPrimaryTable().getPrimaryKey().addColumn( (Column) valueBinding.getValue() );
			}
		}

		public EntityIdentifierNature getNature() {
			return nature;
		}

		public SingularNonAssociationAttributeBinding getAttributeBinding() {
			return identifierAttributeBinding;
		}

		public String getUnsavedValue() {
			return unsavedValue;
		}

		protected IdentifierGeneratorDefinition getIdentifierGeneratorDefinition() {
			return identifierGeneratorDefinition;
		}

		public int getColumnCount() {
			return columnCount;
		}

		public boolean isIdentifierAttributeBinding(AttributeBinding attributeBinding) {
			return getAttributeBinding().equals( attributeBinding );
		}

		public IdentifierGenerator createIdentifierGenerator(
				IdentifierGeneratorFactory identifierGeneratorFactory,
				Properties properties) {
			final List<RelationalValueBinding> relationalValueBindings =
					getAttributeBinding().getRelationalValueBindings();

			// TODO: If multiple @Column annotations exist within an id's
			// @Columns, we need a more solid solution than simply grabbing
			// the first one to get the TableSpecification.

			final RelationalValueBinding relationalValueBinding = relationalValueBindings.get( 0 );
			final TableSpecification table = relationalValueBinding.getTable();
			if ( !Column.class.isInstance( relationalValueBinding.getValue() ) ) {
				throw new MappingException(
						"Cannot create an IdentifierGenerator because the value is not a column: " +
								relationalValueBinding.getValue().toLoggableString()
				);
			}

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

			params.setProperty( IdentifierGenerator.ENTITY_NAME, entityBinding.getEntity().getName() );
			params.setProperty( IdentifierGenerator.JPA_ENTITY_NAME, entityBinding.getJpaEntityName() );

			//init the table here instead of earlier, so that we can get a quoted table name
			//TODO: would it be better to simply pass the qualified table name, instead of
			//      splitting it up into schema/catalog/table names
			String tableName = table.getQualifiedName( identifierGeneratorFactory.getDialect() );
			params.setProperty( PersistentIdentifierGenerator.TABLE, tableName );

			params.setProperty(
					PersistentIdentifierGenerator.PK,
					( (Column) relationalValueBinding.getValue() ).getColumnName().getText(
							identifierGeneratorFactory.getDialect()
					)
			);
			if ( entityBinding.getHierarchyDetails().getInheritanceType() != InheritanceType.TABLE_PER_CLASS ) {
				params.setProperty( PersistentIdentifierGenerator.TABLES, tableName );
			} else {
				params.setProperty(
						PersistentIdentifierGenerator.TABLES,
						resolveTableNames( identifierGeneratorFactory.getDialect(), entityBinding )
				);
			}
			params.putAll( getIdentifierGeneratorDefinition().getParameters() );
			return identifierGeneratorFactory.createIdentifierGenerator(
					getIdentifierGeneratorDefinition().getStrategy(),
					getAttributeBinding().getHibernateTypeDescriptor().getResolvedTypeMapping(),
					params
			);
		}
	}

	private class SimpleAttributeIdentifierBindingImpl extends EntityIdentifierBinding {
		SimpleAttributeIdentifierBindingImpl(
				SingularNonAssociationAttributeBinding identifierAttributeBinding,
				IdentifierGeneratorDefinition identifierGeneratorDefinition,
				String unsavedValue) {
			super( SIMPLE, identifierAttributeBinding, identifierGeneratorDefinition, unsavedValue );
		}

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

	private class AggregatedComponentIdentifierBindingImpl extends EntityIdentifierBinding {
		AggregatedComponentIdentifierBindingImpl(
				CompositeAttributeBinding identifierAttributeBinding,
				IdentifierGeneratorDefinition identifierGeneratorDefinition,
				String unsavedValue) {
			super( AGGREGATED_COMPOSITE, identifierAttributeBinding, identifierGeneratorDefinition, unsavedValue );
			if ( ! identifierAttributeBinding.isAggregated() ) {
				throw new IllegalArgumentException(
						String.format(
								"identifierAttributeBinding must be an aggregated CompositeAttributeBinding: %s",
								identifierAttributeBinding.getAttribute().getName()
						)
				);
			}
		}

		public IdentifierGenerator createIdentifierGenerator(
				IdentifierGeneratorFactory factory,
				Properties properties) {
			if ( entityBinding.getSuperEntityBinding() != null ) {
				throw new AssertionError( "Creating an identifier generator for a component on a subclass." );
			}
			final EntityIdentifier entityIdentifier = entityBinding.getHierarchyDetails().getEntityIdentifier();

			final boolean hasCustomGenerator = ! "assigned".equals( getIdentifierGeneratorDefinition().getStrategy() );
			if ( hasCustomGenerator ) {
				return super.createIdentifierGenerator(
						factory, properties
				);
			}
			final Class entityClass = entityBinding.getEntity().getClassReference();
			final Class attributeDeclarer; // what class is the declarer of the composite pk attributes
			// IMPL NOTE : See the javadoc discussion on CompositeNestedGeneratedValueGenerator wrt the
			//		various scenarios for which we need to account here
			// we have the "@EmbeddedId" / <composite-id name="idName"/> case
			CompositeNestedGeneratedValueGenerator.GenerationContextLocator locator =
					new CompositeNestedGeneratedValueGenerator.GenerationContextLocator() {
						public Serializable locateGenerationContext( SessionImplementor session, Object incomingObject) {
							return session.getEntityPersister( entityBinding.getEntity().getName(), incomingObject )
									.getIdentifier( incomingObject, session );
						}
					};
			// TODO: set up IdentifierGenerator for non-assigned sub-attributes
			return new CompositeNestedGeneratedValueGenerator( locator );
		}
	}

	private static class ValueGenerationPlan implements CompositeNestedGeneratedValueGenerator.GenerationPlan {
		private final String propertyName;
		private final IdentifierGenerator subGenerator;
		private final Setter injector;

		public ValueGenerationPlan(
				String propertyName,
				IdentifierGenerator subGenerator,
				Setter injector) {
			this.propertyName = propertyName;
			this.subGenerator = subGenerator;
			this.injector = injector;
		}

		/**
		 * {@inheritDoc}
		 */
		public void execute(SessionImplementor session, Object incomingObject, Object injectionContext) {
			final Object generatedValue = subGenerator.generate( session, incomingObject );
			injector.set( injectionContext, generatedValue, session.getFactory() );
		}

		public void registerPersistentGenerators(Map generatorMap) {
			if ( PersistentIdentifierGenerator.class.isInstance( subGenerator ) ) {
				generatorMap.put( ( (PersistentIdentifierGenerator) subGenerator ).generatorKey(), subGenerator );
			}
		}
	}

	private class NonAggregatedCompositeIdentifierBindingImpl extends EntityIdentifierBinding {
		private final Class<?> externalAggregatingClass;
		private final String externalAggregatingPropertyAccessorName;

		NonAggregatedCompositeIdentifierBindingImpl(
				CompositeAttributeBinding identifierAttributeBinding,
				IdentifierGeneratorDefinition identifierGeneratorDefinition,
				String unsavedValue,
				Class<?> externalAggregatingClass,
				String externalAggregatingPropertyAccessorName) {
			super( NON_AGGREGATED_COMPOSITE, identifierAttributeBinding, identifierGeneratorDefinition, unsavedValue );
			if ( identifierAttributeBinding.isAggregated() ) {
				throw new IllegalArgumentException(
						String.format(
								"identifierAttributeBinding must be a non-aggregated CompositeAttributeBinding: %s",
								identifierAttributeBinding.getAttribute().getName()
						)
				);
			}
			this.externalAggregatingClass = externalAggregatingClass;
			this.externalAggregatingPropertyAccessorName = externalAggregatingPropertyAccessorName;
			if ( identifierAttributeBinding.attributeBindingSpan() == 0 ) {
				throw new MappingException(
						"A composite ID has 0 attributes for " + entityBinding.getEntity().getName()
				);
			}
			for ( AttributeBinding attributeBinding : identifierAttributeBinding.attributeBindings() ) {
				if ( ! attributeBinding.getAttribute().isSingular() ) {
					throw new MappingException(
							String.format(
									"The composite ID for [%s] contains an attribute [%s} that is plural.",
									entityBinding.getEntity().getName(),
									attributeBinding.getAttribute().getName()
							)
					);
				}
			}
		}

		private CompositeAttributeBinding getNonAggregatedCompositeAttributeBinding() {
			return (CompositeAttributeBinding) getAttributeBinding();
		}
		public boolean isIdentifierAttributeBinding(AttributeBinding attributeBinding) {
			if ( !isIdentifierMapper() && getNonAggregatedCompositeAttributeBinding().equals( attributeBinding ) ) {
				return true;

			}
			for ( AttributeBinding idAttributeBindings : getNonAggregatedCompositeAttributeBinding().attributeBindings() ) {
				if ( idAttributeBindings.equals( attributeBinding ) ) {
					return true;
				}
			}
			return false;
		}

		public Class getIdClassClass() {
			return externalAggregatingClass;
		}

		public String getIdClassPropertyAccessorName() {
			return externalAggregatingPropertyAccessorName;
		}

		public IdentifierGenerator createIdentifierGenerator(
				IdentifierGeneratorFactory factory,
				Properties properties) {
			if ( entityBinding.getSuperEntityBinding() != null ) {
				throw new AssertionError( "Creating an identifier generator for a component on a subclass." );
			}
			final EntityIdentifier entityIdentifier = entityBinding.getHierarchyDetails().getEntityIdentifier();
			final Class entityClass = entityBinding.getEntity().getClassReference();
			final Class attributeDeclarer; // what class is the declarer of the composite pk attributes
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
							return session.getEntityPersister( entityBinding.getEntity().getName(), incomingObject )
									.getIdentifier( incomingObject, session );
						}
					};
			// TODO: set up IdentifierGenerator for non-assigned sub-attributes
			return new CompositeNestedGeneratedValueGenerator( locator );
		}
	}

}
