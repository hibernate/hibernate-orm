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
package org.hibernate.metamodel.internal.source.annotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jaxb.spi.Origin;
import org.hibernate.metamodel.internal.source.annotations.attribute.AssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.AttributeOverride;
import org.hibernate.metamodel.internal.source.annotations.attribute.BasicAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.entity.EmbeddableClass;
import org.hibernate.metamodel.internal.source.annotations.entity.EntityClass;
import org.hibernate.metamodel.internal.source.annotations.entity.RootEntityClass;
import org.hibernate.metamodel.internal.source.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.CustomSQL;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.ConstraintSource;
import org.hibernate.metamodel.spi.source.EntitySource;
import org.hibernate.metamodel.spi.source.JpaCallbackSource;
import org.hibernate.metamodel.spi.source.LocalBindingContext;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.PrimaryKeyJoinColumnSource;
import org.hibernate.metamodel.spi.source.SecondaryTableSource;
import org.hibernate.metamodel.spi.source.SubclassEntitySource;
import org.hibernate.metamodel.spi.source.TableSpecificationSource;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

/**
 * @author Hardy Ferentschik
 */
public class EntitySourceImpl implements EntitySource {
	private final EntityClass entityClass;
	private final Set<SubclassEntitySource> subclassEntitySources;
	private final String jpaEntityName;

	public EntitySourceImpl(EntityClass entityClass) {
		this.entityClass = entityClass;
		this.subclassEntitySources = new HashSet<SubclassEntitySource>();

		if ( StringHelper.isNotEmpty( entityClass.getExplicitEntityName() ) ) {
			this.jpaEntityName = entityClass.getExplicitEntityName();
		}
		else {
			this.jpaEntityName = StringHelper.unqualify( entityClass.getName() );
		}

		addImports();
	}

	public EntityClass getEntityClass() {
		return entityClass;
	}

	@Override
	public Origin getOrigin() {
		return entityClass.getLocalBindingContext().getOrigin();
	}

	@Override
	public LocalBindingContext getLocalBindingContext() {
		return entityClass.getLocalBindingContext();
	}

	@Override
	public String getEntityName() {
		return getClassName();
	}

	@Override
	public String getClassName() {
		return entityClass.getName();
	}

	@Override
	public String getJpaEntityName() {
		return jpaEntityName;
	}

	@Override
	public TableSpecificationSource getPrimaryTable() {
		if ( !entityClass.definesItsOwnTable() ) {
			return null;
		}

		if ( entityClass.hostsAnnotation( HibernateDotNames.SUB_SELECT ) ) {
			return new InLineViewSourceImpl( entityClass );
		}
		else {
			AnnotationInstance tableAnnotation = JandexHelper.getSingleAnnotation(
					entityClass.getClassInfo(),
					JPADotNames.TABLE
			);
			return new TableSourceImpl( tableAnnotation );
		}
	}

	@Override
	public boolean isAbstract() {
		return false;
	}

	@Override
	public boolean isLazy() {
		return entityClass.isLazy();
	}

	@Override
	public String getProxy() {
		return entityClass.getProxy();
	}

	@Override
	public int getBatchSize() {
		return entityClass.getBatchSize();
	}

	@Override
	public boolean isDynamicInsert() {
		return entityClass.isDynamicInsert();
	}

	@Override
	public boolean isDynamicUpdate() {
		return entityClass.isDynamicUpdate();
	}

	@Override
	public boolean isSelectBeforeUpdate() {
		return entityClass.isSelectBeforeUpdate();
	}

	@Override
	public String getCustomTuplizerClassName() {
		return entityClass.getCustomTuplizer();
	}

	@Override
	public String getCustomPersisterClassName() {
		return entityClass.getCustomPersister();
	}

	@Override
	public String getCustomLoaderName() {
		return entityClass.getCustomLoaderQueryName();
	}

	@Override
	public CustomSQL getCustomSqlInsert() {
		return entityClass.getCustomInsert();
	}

	@Override
	public CustomSQL getCustomSqlUpdate() {
		return entityClass.getCustomUpdate();
	}

	@Override
	public CustomSQL getCustomSqlDelete() {
		return entityClass.getCustomDelete();
	}

	@Override
	public List<String> getSynchronizedTableNames() {
		return entityClass.getSynchronizedTableNames();
	}

	@Override
	public Iterable<MetaAttributeSource> getMetaAttributeSources() {
		// not relevant for annotations
		return Collections.emptySet();
	}

	@Override
	public String getPath() {
		return entityClass.getName();
	}

	@Override
	public List<AttributeSource> attributeSources() {
		List<AttributeSource> attributeList = new ArrayList<AttributeSource>();
		for ( BasicAttribute attribute : entityClass.getSimpleAttributes() ) {
			AttributeOverride override = getEntityClass().getAttributeOverrideMap().get( attribute.getName() );
			attributeList.add( new SingularAttributeSourceImpl( attribute , override ));
		}

		for ( EmbeddableClass component : entityClass.getEmbeddedClasses().values() ) {
			attributeList.add(
					new ComponentAttributeSourceImpl(
							component,
							"",
							entityClass.getAttributeOverrideMap()
					)
			);
		}

		for ( AssociationAttribute associationAttribute : entityClass.getAssociationAttributes() ) {
			switch ( associationAttribute.getNature() ) {
				case ONE_TO_ONE:
				case MANY_TO_ONE: {
					attributeList.add( new ToOneAttributeSourceImpl( associationAttribute ) );
					break;
				}
				case MANY_TO_MANY:
				case ONE_TO_MANY:
					AttributeSource source = ((PluralAssociationAttribute)associationAttribute).isIndexed() ?
							new IndexedPluralAttributeSourceImpl((PluralAssociationAttribute) associationAttribute, entityClass )
							:new PluralAttributeSourceImpl( ( PluralAssociationAttribute ) associationAttribute, entityClass );
					attributeList.add( source );
					break;
				case ELEMENT_COLLECTION_BASIC:
				case ELEMENT_COLLECTION_EMBEDDABLE: {
					source = new PluralAttributeSourceImpl( ( PluralAssociationAttribute ) associationAttribute, entityClass );
					attributeList.add( source );
					break;
				}
				default: {
					throw new NotYetImplementedException();
				}
			}
		}
		return attributeList;
	}

	@Override
	public void add(SubclassEntitySource subclassEntitySource) {
		subclassEntitySources.add( subclassEntitySource );
	}

	@Override
	public Iterable<SubclassEntitySource> subclassEntitySources() {
		return subclassEntitySources;
	}

	@Override
	public String getDiscriminatorMatchValue() {
		return entityClass.getDiscriminatorMatchValue();
	}

	@Override
	public Iterable<ConstraintSource> getConstraints() {
		Set<ConstraintSource> constraintSources = new HashSet<ConstraintSource>();

		// primary table
		if ( entityClass.hostsAnnotation( JPADotNames.TABLE ) ) {
			AnnotationInstance table = JandexHelper.getSingleAnnotation(
					entityClass.getClassInfo(),
					JPADotNames.TABLE
			);
			addUniqueConstraints( constraintSources, table, null );
		}

		// secondary table(s)
		if ( entityClass.hostsAnnotation( JPADotNames.SECONDARY_TABLE ) ) {
			AnnotationInstance secondaryTable = JandexHelper.getSingleAnnotation(
					entityClass.getClassInfo(),
					JPADotNames.SECONDARY_TABLE
			);
			String tableName = JandexHelper.getValue( secondaryTable, "name", String.class );
			addUniqueConstraints( constraintSources, secondaryTable, tableName );

		}

		if ( entityClass.hostsAnnotation( JPADotNames.SECONDARY_TABLES ) ) {
			AnnotationInstance secondaryTables = JandexHelper.getSingleAnnotation(
					entityClass.getClassInfo(),
					JPADotNames.SECONDARY_TABLES
			);
			if ( secondaryTables != null ) {
				for ( AnnotationInstance secondaryTable : JandexHelper.getValue(
						secondaryTables,
						"value",
						AnnotationInstance[].class
				) ) {
					String tableName = JandexHelper.getValue( secondaryTable, "name", String.class );
					addUniqueConstraints( constraintSources, secondaryTable, tableName );
				}
			}
		}

		return constraintSources;
	}

	@Override
	public List<JpaCallbackSource> getJpaCallbackClasses() {
		return entityClass.getJpaCallbacks();
	}

	@Override
	public Set<SecondaryTableSource> getSecondaryTables() {
		Set<SecondaryTableSource> secondaryTableSources = new HashSet<SecondaryTableSource>();

		//	process a singular @SecondaryTable annotation
		if ( entityClass.hostsAnnotation( JPADotNames.SECONDARY_TABLE ) ) {
			AnnotationInstance secondaryTable = JandexHelper.getSingleAnnotation(
					entityClass.getClassInfo(),
					JPADotNames.SECONDARY_TABLE
			);
			if ( secondaryTable != null ) {
				secondaryTableSources.add( createSecondaryTableSource( secondaryTable ) );
			}
		}

		// process any @SecondaryTables grouping
		if ( entityClass.hostsAnnotation( JPADotNames.SECONDARY_TABLES ) ) {
			AnnotationInstance secondaryTables = JandexHelper.getSingleAnnotation(
					entityClass.getClassInfo(),
					JPADotNames.SECONDARY_TABLES
			);
			if ( secondaryTables != null ) {
				AnnotationInstance[] tableAnnotations = JandexHelper.getValue(
						secondaryTables,
						"value",
						AnnotationInstance[].class
				);
				for ( AnnotationInstance secondaryTable : tableAnnotations ) {
					secondaryTableSources.add( createSecondaryTableSource( secondaryTable ) );
				}
			}
		}

		return secondaryTableSources;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "EntitySourceImpl" );
		sb.append( "{entityClass=" ).append( entityClass.getName() );

		sb.append( ", subclassEntitySources={" );
		for ( SubclassEntitySource subClass : subclassEntitySources ) {
			sb.append( subClass.getClassName() ).append( "," );
		}
		sb.append( "}}" );
		return sb.toString();
	}

	private void addImports() {
		try {
			final MetadataImplementor metadataImplementor = entityClass.getLocalBindingContext()
					.getMetadataImplementor();
			metadataImplementor.addImport( getJpaEntityName(), getEntityName() );
			if ( !getEntityName().equals( getJpaEntityName() ) ) {
				metadataImplementor.addImport( getEntityName(), getEntityName() );
			}
		}
		catch ( MappingException e ) {
			throw new AnnotationException( "Use of the same entity name twice: " + getJpaEntityName(), e );
		}
	}

	private void addUniqueConstraints(Set<ConstraintSource> constraintSources, AnnotationInstance tableAnnotation, String tableName) {
		final AnnotationValue value = tableAnnotation.value( "uniqueConstraints" );
		if ( value == null ) {
			return;
		}

		final AnnotationInstance[] uniqueConstraints = value.asNestedArray();
		for ( final AnnotationInstance unique : uniqueConstraints ) {
			final String name = unique.value( "name" ) == null ? null : unique.value( "name" ).asString();
			final String[] columnNames = unique.value( "columnNames" ).asStringArray();
			final UniqueConstraintSourceImpl uniqueConstraintSource =
					new UniqueConstraintSourceImpl(
							name, tableName, Arrays.asList( columnNames )
					);
			constraintSources.add( uniqueConstraintSource );
		}
	}

	private SecondaryTableSource createSecondaryTableSource(AnnotationInstance tableAnnotation) {
		final List<PrimaryKeyJoinColumnSource> keys = collectionSecondaryTableKeys( tableAnnotation );
		return new SecondaryTableSourceImpl( new TableSourceImpl( tableAnnotation ), keys );
	}

	private List<PrimaryKeyJoinColumnSource> collectionSecondaryTableKeys(final AnnotationInstance tableAnnotation) {
		final AnnotationInstance[] joinColumnAnnotations = JandexHelper.getValue(
				tableAnnotation,
				"pkJoinColumns",
				AnnotationInstance[].class
		);

		if ( joinColumnAnnotations == null ) {
			return Collections.emptyList();
		}
		final List<PrimaryKeyJoinColumnSource> keys = new ArrayList<PrimaryKeyJoinColumnSource>();
		for ( final AnnotationInstance joinColumnAnnotation : joinColumnAnnotations ) {
			keys.add( new PrimaryKeyJoinColumnSourceImpl( joinColumnAnnotation ) );
		}
		return keys;
	}
}


