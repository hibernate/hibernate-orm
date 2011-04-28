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
package org.hibernate.metamodel.source.annotations;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.binding.SimpleAttributeBinding;
import org.hibernate.metamodel.domain.Attribute;
import org.hibernate.metamodel.domain.Entity;
import org.hibernate.metamodel.domain.Hierarchical;
import org.hibernate.metamodel.relational.Schema;
import org.hibernate.metamodel.relational.Size;
import org.hibernate.metamodel.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.source.internal.MetadataImpl;

/**
 * Creates the domain and relational metamodel for a configured class and binds them together.
 *
 * @author Hardy Ferentschik
 */
public class EntityBinder {
	private final ConfiguredClass entity;
	private final MetadataImpl meta;

	public EntityBinder(MetadataImpl metadata, ConfiguredClass configuredClass) {
		this.entity = configuredClass;
		this.meta = metadata;
		EntityBinding entityBinding = new EntityBinding();
		bindJpaEntityAnnotation( entityBinding );
		bindHibernateEntityAnnotation( entityBinding ); // optional hibernate specific @org.hibernate.annotations.Entity
		bindTable( entityBinding );

		if ( configuredClass.isRoot() ) {
			bindId( entityBinding );
		}
		meta.addEntity( entityBinding );
	}

	private void bindTable(EntityBinding entityBinding) {
		final Schema schema = meta.getDatabase().getSchema( null );
	}

	private void bindId(EntityBinding entityBinding) {
		switch ( determineIdType() ) {
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

	private void bindJpaEntityAnnotation(EntityBinding entityBinding) {
		AnnotationInstance jpaEntityAnnotation = JandexHelper.getSingleAnnotation(
				entity.getClassInfo(), JPADotNames.ENTITY
		);
		String name;
		if ( jpaEntityAnnotation.value( "name" ) == null ) {
			name = StringHelper.unqualify( entity.getName() );
		}
		else {
			name = jpaEntityAnnotation.value( "name" ).asString();
		}
		entityBinding.setEntity( new Entity( name, getSuperType() ) );
	}

	private void bindSingleIdAnnotation(EntityBinding entityBinding) {
		AnnotationInstance idAnnotation = JandexHelper.getSingleAnnotation(
				entity.getClassInfo(), JPADotNames.ID
		);

		String idName = JandexHelper.getPropertyName( idAnnotation.target() );
		entityBinding.getEntity().getOrCreateSingularAttribute( idName );
		SimpleAttributeBinding idBinding = entityBinding.makeSimplePrimaryKeyAttributeBinding( idName );

		MappedProperty idProperty = entity.getMappedProperty( idName );

		AnnotationSimpleAttributeDomainState domainState = new AnnotationSimpleAttributeDomainState();
		HibernateTypeDescriptor typeDescriptor = new HibernateTypeDescriptor();
		typeDescriptor.setTypeName( idProperty.getType().getName() );
		domainState.typeDescriptor = typeDescriptor;
		domainState.attribute = entityBinding.getEntity().getOrCreateSingularAttribute( idProperty.getName() );

		idBinding.initialize( domainState );

		AnnotationColumnRelationalState columnRelationsState = new AnnotationColumnRelationalState();
		columnRelationsState.namingStrategy = meta.getNamingStrategy();
		columnRelationsState.columnName = idProperty.getColumnName();
		columnRelationsState.unique = true;
		columnRelationsState.nullable = false;

		AnnotationSimpleAttributeRelationalState relationalState = new AnnotationSimpleAttributeRelationalState();
		relationalState.valueStates.add( columnRelationsState );
		idBinding.initializeTupleValue( relationalState );
	}

	private void bindHibernateEntityAnnotation(EntityBinding entityBinding) {
		AnnotationInstance hibernateEntityAnnotation = JandexHelper.getSingleAnnotation(
				entity.getClassInfo(), HibernateDotNames.ENTITY
		);
//		if ( hibAnn != null ) {
//			dynamicInsert = hibAnn.dynamicInsert();
//			dynamicUpdate = hibAnn.dynamicUpdate();
//			optimisticLockType = hibAnn.optimisticLock();
//			selectBeforeUpdate = hibAnn.selectBeforeUpdate();
//			polymorphismType = hibAnn.polymorphism();
//			explicitHibernateEntityAnnotation = true;
//			//persister handled in bind
//		}
//		else {
//			//default values when the annotation is not there
//			dynamicInsert = false;
//			dynamicUpdate = false;
//			optimisticLockType = OptimisticLockType.VERSION;
//			polymorphismType = PolymorphismType.IMPLICIT;
//			selectBeforeUpdate = false;
//		}
	}

	private Hierarchical getSuperType() {
		ConfiguredClass parent = entity.getParent();
		if ( parent == null ) {
			return null;
		}

		EntityBinding parentBinding = meta.getEntityBinding( parent.getName() );
		if ( parentBinding == null ) {
			throw new AssertionFailure(
					"Parent entity " + parent.getName() + " of entity " + entity.getName() + "not yet created!"
			);
		}

		return parentBinding.getEntity();
	}

	private IdType determineIdType() {
		List<AnnotationInstance> idAnnotations = entity.getClassInfo().annotations().get( JPADotNames.ENTITY );
		List<AnnotationInstance> embeddedIdAnnotations = entity.getClassInfo()
				.annotations()
				.get( JPADotNames.EMBEDDED_ID );

		if ( idAnnotations != null && embeddedIdAnnotations != null ) {
			throw new MappingException(
					"@EmbeddedId and @Id cannot be used together. Check the configuration for " + entity.getName() + "."
			);
		}

		if ( embeddedIdAnnotations != null ) {
			if ( embeddedIdAnnotations.size() == 1 ) {
				return IdType.EMBEDDED;
			}
			else {
				throw new MappingException( "Multiple @EmbeddedId annotations are not allowed" );
			}
		}

		if ( idAnnotations != null ) {
			if ( idAnnotations.size() == 1 ) {
				return IdType.SIMPLE;
			}
			else {
				return IdType.COMPOSED;
			}
		}

		return IdType.NONE;
	}

	enum IdType {
		// single @Id annotation
		SIMPLE,
		// multiple @Id annotations
		COMPOSED,
		// @EmbeddedId annotation
		EMBEDDED,
		// does not contain any identifier mappings
		NONE
	}

	public static class AnnotationSimpleAttributeDomainState implements SimpleAttributeBinding.DomainState {
		PropertyGeneration propertyGeneration;
		HibernateTypeDescriptor typeDescriptor;
		Attribute attribute;

		@Override
		public PropertyGeneration getPropertyGeneration() {

//		GeneratedValue generatedValue = property.getAnnotation( GeneratedValue.class );
//		String generatorType = generatedValue != null ?
//				generatorType( generatedValue.strategy(), mappings ) :
//				"assigned";
//		String generatorName = generatedValue != null ?
//				generatedValue.generator() :
//				BinderHelper.ANNOTATION_STRING_DEFAULT;
			return propertyGeneration;
		}

		@Override
		public boolean isInsertable() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public boolean isUpdateable() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public boolean isKeyCasadeDeleteEnabled() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public String getUnsavedValue() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public HibernateTypeDescriptor getHibernateTypeDescriptor() {
			return typeDescriptor;
		}

		@Override
		public Attribute getAttribute() {
			return attribute;
		}

		@Override
		public boolean isLazy() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public String getPropertyAccessorName() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public boolean isAlternateUniqueKey() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public String getCascade() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public boolean isOptimisticLockable() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public String getNodeName() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public Map<String, org.hibernate.metamodel.domain.MetaAttribute> getMetaAttributes(EntityBinding entityBinding) {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}
	}

	public static class AnnotationSimpleAttributeRelationalState
			implements SimpleAttributeBinding.TupleRelationalState {
		LinkedHashSet<SimpleAttributeBinding.SingleValueRelationalState> valueStates = new LinkedHashSet<SimpleAttributeBinding.SingleValueRelationalState>();

		@Override
		public LinkedHashSet<SimpleAttributeBinding.SingleValueRelationalState> getSingleValueRelationalStates() {
			return valueStates;
		}
	}

	public static class AnnotationColumnRelationalState
			implements SimpleAttributeBinding.ColumnRelationalState {

		NamingStrategy namingStrategy;
		String columnName;
		boolean unique;
		boolean nullable;

		@Override
		public NamingStrategy getNamingStrategy() {
			return namingStrategy;
		}

		@Override
		public String getExplicitColumnName() {
			return columnName;
		}

		@Override
		public boolean isUnique() {
			return unique;
		}

		@Override
		public Size getSize() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public boolean isNullable() {
			return nullable;
		}

		@Override
		public String getCheckCondition() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public String getDefault() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public String getSqlType() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public String getCustomWriteFragment() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public String getCustomReadFragment() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public String getComment() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public Set<String> getUniqueKeys() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public Set<String> getIndexes() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}
	}
}



