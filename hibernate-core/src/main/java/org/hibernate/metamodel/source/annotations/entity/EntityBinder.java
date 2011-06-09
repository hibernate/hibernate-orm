/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.annotations.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.GenerationType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.EntityDiscriminator;
import org.hibernate.metamodel.binding.IdGenerator;
import org.hibernate.metamodel.binding.ManyToOneAttributeBinding;
import org.hibernate.metamodel.binding.SimpleAttributeBinding;
import org.hibernate.metamodel.binding.state.DiscriminatorBindingState;
import org.hibernate.metamodel.binding.state.EntityBindingState;
import org.hibernate.metamodel.binding.state.ManyToOneAttributeBindingState;
import org.hibernate.metamodel.binding.state.SimpleAttributeBindingState;
import org.hibernate.metamodel.domain.Entity;
import org.hibernate.metamodel.domain.Hierarchical;
import org.hibernate.metamodel.relational.Identifier;
import org.hibernate.metamodel.relational.Schema;
import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.entity.state.binding.AttributeBindingStateImpl;
import org.hibernate.metamodel.source.annotations.entity.state.binding.DiscriminatorBindingStateImpl;
import org.hibernate.metamodel.source.annotations.entity.state.binding.EntityBindingStateImpl;
import org.hibernate.metamodel.source.annotations.entity.state.binding.ManyToOneBindingStateImpl;
import org.hibernate.metamodel.source.annotations.entity.state.relational.ColumnRelationalStateImpl;
import org.hibernate.metamodel.source.annotations.entity.state.relational.ManyToOneRelationalStateImpl;
import org.hibernate.metamodel.source.annotations.entity.state.relational.TupleRelationalStateImpl;
import org.hibernate.metamodel.source.annotations.global.IdGeneratorBinder;
import org.hibernate.metamodel.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.source.spi.MetadataImplementor;

/**
 * Creates the domain and relational metamodel for a configured class and <i>binds</i> them together.
 *
 * @author Hardy Ferentschik
 */
public class EntityBinder {
	private final ConfiguredClass configuredClass;
	private final MetadataImplementor meta;

	private Schema.Name schemaName;

	public EntityBinder(MetadataImplementor metadata, ConfiguredClass configuredClass) {
		this.configuredClass = configuredClass;
		this.meta = metadata;
	}

	public void bind() {
		EntityBinding entityBinding = new EntityBinding();
		initializeEntityBinding( entityBinding );

		schemaName = createSchemaName();
		bindTable( entityBinding );

		bindDiscriminator( entityBinding );

		// take care of the id, attributes and relations
		if ( configuredClass.isRoot() ) {
			bindId( entityBinding );
		}

		// bind all attributes - simple as well as associations
		bindAttributes( entityBinding );

		// last, but not least we register the new EntityBinding with the metadata
		meta.addEntity( entityBinding );
	}

	private void bindDiscriminator(EntityBinding entityBinding) {
		switch ( configuredClass.getInheritanceType() ) {
			case SINGLE_TABLE: {
				bindDiscriminatorColumn( entityBinding );
				break;
			}
			case JOINED: {
				// todo
				break;
			}
			case TABLE_PER_CLASS: {
				// todo
				break;
			}
			default: {
				// do nothing
			}
		}
	}

	private void bindDiscriminatorColumn(EntityBinding entityBinding) {
		final Map<DotName, List<AnnotationInstance>> typeAnnotations = JandexHelper.getTypeAnnotations(
				configuredClass.getClassInfo()
		);
		SimpleAttribute discriminatorAttribute = SimpleAttribute.createDiscriminatorAttribute( typeAnnotations );

		bindSingleMappedAttribute( entityBinding, discriminatorAttribute );

		if ( !( discriminatorAttribute.getColumnValues() instanceof DiscriminatorColumnValues ) ) {
			throw new AssertionFailure( "Expected discriminator column values" );
		}
	}

	private Schema.Name createSchemaName() {
		String schema = null;
		String catalog = null;

		AnnotationInstance tableAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), JPADotNames.TABLE
		);
		if ( tableAnnotation != null ) {
			AnnotationValue schemaValue = tableAnnotation.value( "schema" );
			AnnotationValue catalogValue = tableAnnotation.value( "catalog" );

			schema = schemaValue != null ? schemaValue.asString() : null;
			catalog = catalogValue != null ? catalogValue.asString() : null;
		}

		return new Schema.Name( schema, catalog );
	}

	private void bindTable(EntityBinding entityBinding) {
		final Schema schema = meta.getDatabase().getSchema( schemaName );
		final Identifier tableName = Identifier.toIdentifier( configuredClass.getPrimaryTableName() );
		org.hibernate.metamodel.relational.Table table = schema.getTable( tableName );
		if ( table == null ) {
			table = schema.createTable( tableName );
		}
		entityBinding.setBaseTable( table );

		AnnotationInstance checkAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.CHECK
		);
		if ( checkAnnotation != null ) {
			table.addCheckConstraint( checkAnnotation.value( "constraints" ).asString() );
		}
	}

	private void bindId(EntityBinding entityBinding) {
		switch ( configuredClass.getIdType() ) {
			case SIMPLE: {
				bindSingleIdAnnotation( entityBinding );
				break;
			}
			case COMPOSED: {
				// todo
				break;
			}
			case EMBEDDED: {
				// todo
				break;
			}
			default: {
			}
		}
	}

	private void initializeEntityBinding(EntityBinding entityBinding) {
		entityBinding.setEntity( new Entity( getEntityName( configuredClass ), getSuperType() ) );
		EntityBindingState entityBindingState = new EntityBindingStateImpl(
				meta, configuredClass, entityBinding.getEntity().getName()
		);
		entityBinding.initialize( entityBindingState );
	}

	private static String getEntityName(ConfiguredClass configuredClass) {
		AnnotationInstance jpaEntityAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), JPADotNames.ENTITY
		);
		String name;
		if ( jpaEntityAnnotation.value( "name" ) == null ) {
			name = configuredClass.getName();
		}
		else {
			name = jpaEntityAnnotation.value( "name" ).asString();
		}
		return name;
	}

	private void bindSingleIdAnnotation(EntityBinding entityBinding) {
		AnnotationInstance idAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), JPADotNames.ID
		);

		String idName = JandexHelper.getPropertyName( idAnnotation.target() );
		MappedAttribute idAttribute = configuredClass.getMappedProperty( idName );
		if ( !( idAttribute instanceof SimpleAttribute ) ) {
			throw new AssertionFailure( "Unexpected attribute type for id attribute" );
		}

		entityBinding.getEntity().getOrCreateSingularAttribute( idName );

		SimpleAttributeBinding attributeBinding = entityBinding.makeSimpleIdAttributeBinding( idName );
		attributeBinding.initialize( new AttributeBindingStateImpl( (SimpleAttribute) idAttribute ) );
		attributeBinding.initialize( new ColumnRelationalStateImpl( (SimpleAttribute) idAttribute, meta ) );
		bindSingleIdGeneratedValue( entityBinding, idName );

	}

	private void bindSingleIdGeneratedValue(EntityBinding entityBinding, String idPropertyName) {
		AnnotationInstance generatedValueAnn = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), JPADotNames.GENERATED_VALUE
		);
		if ( generatedValueAnn == null ) {
			return;
		}

		String idName = JandexHelper.getPropertyName( generatedValueAnn.target() );
		if ( !idPropertyName.equals( idName ) ) {
			throw new AssertionFailure(
					String.format(
							"Attribute[%s.%s] with @GeneratedValue doesn't have a @Id.",
							configuredClass.getName(),
							idPropertyName
					)
			);
		}
		String generator = JandexHelper.getValueAsString( generatedValueAnn, "generator" );
		IdGenerator idGenerator = null;
		if ( StringHelper.isNotEmpty( generator ) ) {
			idGenerator = meta.getIdGenerator( generator );
			if ( idGenerator == null ) {
				throw new MappingException(
						String.format(
								"@GeneratedValue on %s.%s refering an undefined generator [%s]",
								configuredClass.getName(),
								idName,
								generator
						)
				);
			}
			entityBinding.getEntityIdentifier().setIdGenerator( idGenerator );
		}
		GenerationType generationType = JandexHelper.getValueAsEnum(
				generatedValueAnn,
				"strategy",
				GenerationType.class
		);
		String strategy = IdGeneratorBinder.generatorType(
				generationType,
				meta.getOptions().useNewIdentifierGenerators()
		);
		if ( idGenerator != null && !strategy.equals( idGenerator.getStrategy() ) ) {
			//todo how to ?
			throw new MappingException(
					String.format(
							"Inconsistent Id Generation strategy of @GeneratedValue on %s.%s",
							configuredClass.getName(),
							idName
					)
			);
		}
		else {
			idGenerator = new IdGenerator( "NAME", strategy, new HashMap<String, String>() );
			entityBinding.getEntityIdentifier().setIdGenerator( idGenerator );
		}
	}


	private void bindAttributes(EntityBinding entityBinding) {
		for ( MappedAttribute mappedAttribute : configuredClass.getMappedAttributes() ) {
			if ( mappedAttribute instanceof AssociationAttribute ) {
				bindAssociationAttribute( entityBinding, (AssociationAttribute) mappedAttribute );
			}
			else {
				bindSingleMappedAttribute( entityBinding, (SimpleAttribute) mappedAttribute );
			}
		}
	}

	private void bindAssociationAttribute(EntityBinding entityBinding, AssociationAttribute associationAttribute) {
		switch ( associationAttribute.getAssociationType() ) {
			case MANY_TO_ONE: {
				entityBinding.getEntity().getOrCreateSingularAttribute( associationAttribute.getName() );
				ManyToOneAttributeBinding manyToOneAttributeBinding = entityBinding.makeManyToOneAttributeBinding(
						associationAttribute.getName()
				);

				ManyToOneAttributeBindingState bindingState = new ManyToOneBindingStateImpl( associationAttribute );
				manyToOneAttributeBinding.initialize( bindingState );

				ManyToOneRelationalStateImpl relationalState = new ManyToOneRelationalStateImpl();
				if ( configuredClass.hasOwnTable() ) {
					ColumnRelationalStateImpl columnRelationsState = new ColumnRelationalStateImpl(
							associationAttribute, meta
					);
					relationalState.addValueState( columnRelationsState );
				}
				manyToOneAttributeBinding.initialize( relationalState );
				break;
			}
			default: {
				// todo
			}
		}
	}

	private void bindSingleMappedAttribute(EntityBinding entityBinding, SimpleAttribute simpleAttribute) {
		if ( simpleAttribute.isId() ) {
			return;
		}

		String attributeName = simpleAttribute.getName();
		entityBinding.getEntity().getOrCreateSingularAttribute( attributeName );
		SimpleAttributeBinding attributeBinding;

		if ( simpleAttribute.isDiscriminator() ) {
			EntityDiscriminator entityDiscriminator = entityBinding.makeEntityDiscriminator( attributeName );
			DiscriminatorBindingState bindingState = new DiscriminatorBindingStateImpl( simpleAttribute );
			entityDiscriminator.initialize( bindingState );
			attributeBinding = entityDiscriminator.getValueBinding();
		}
		else if ( simpleAttribute.isVersioned() ) {
			attributeBinding = entityBinding.makeVersionBinding( attributeName );
			SimpleAttributeBindingState bindingState = new AttributeBindingStateImpl( simpleAttribute );
			attributeBinding.initialize( bindingState );
		}
		else {
			attributeBinding = entityBinding.makeSimpleAttributeBinding( attributeName );
			SimpleAttributeBindingState bindingState = new AttributeBindingStateImpl( simpleAttribute );
			attributeBinding.initialize( bindingState );
		}

		if ( configuredClass.hasOwnTable() ) {
			ColumnRelationalStateImpl columnRelationsState = new ColumnRelationalStateImpl(
					simpleAttribute, meta
			);
			TupleRelationalStateImpl relationalState = new TupleRelationalStateImpl();
			relationalState.addValueState( columnRelationsState );

			attributeBinding.initialize( relationalState );
		}
	}

	private Hierarchical getSuperType() {
		ConfiguredClass parent = configuredClass.getParent();
		if ( parent == null ) {
			return null;
		}

		EntityBinding parentBinding = meta.getEntityBinding( parent.getName() );
		if ( parentBinding == null ) {
			throw new AssertionFailure(
					"Parent entity " + parent.getName() + " of entity " + configuredClass.getName() + " not yet created!"
			);
		}

		return parentBinding.getEntity();
	}
}

