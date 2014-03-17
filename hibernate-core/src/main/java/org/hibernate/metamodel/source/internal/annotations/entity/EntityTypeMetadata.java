/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.internal.annotations.entity;

import static org.hibernate.metamodel.source.internal.annotations.util.JPADotNames.ENTITY;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.AccessType;

import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.internal.binder.ForeignKeyDelegate;
import org.hibernate.metamodel.reflite.spi.ClassDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.internal.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.source.internal.annotations.attribute.PrimaryKeyJoinColumn;
import org.hibernate.metamodel.source.internal.annotations.util.AnnotationParserHelper;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.CustomSQL;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;

/**
 * Representation of metadata (configured via annotations or orm.xml) attached
 * to an Entity.
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
public class EntityTypeMetadata extends IdentifiableTypeMetadata {
	private final String explicitEntityName;
	private final String customLoaderQueryName;
	private final String[] synchronizedTableNames;
	private final int batchSize;
	private final String customPersister;
	private final boolean isDynamicInsert;
	private final boolean isDynamicUpdate;
	private final boolean isSelectBeforeUpdate;
	private final CustomSQL customInsert;
	private final CustomSQL customUpdate;
	private final CustomSQL customDelete;
	private final String discriminatorMatchValue;
	private final boolean isLazy;
	private final String proxy;
	private final ForeignKeyDelegate foreignKeyDelegate;

	private final ClassLoaderService classLoaderService;

	// todo : ???
	private final OnDeleteAction onDeleteAction;
	private final List<PrimaryKeyJoinColumn> joinedSubclassPrimaryKeyJoinColumnSources;


	/**
	 * This form is intended for construction of root Entity.
	 */
	public EntityTypeMetadata(
			JavaTypeDescriptor javaTypeDescriptor,
			AccessType defaultAccessType,
			AnnotationBindingContext bindingContext) {
		super( javaTypeDescriptor, defaultAccessType, true, bindingContext );
		
		this.classLoaderService = bindingContext.getServiceRegistry().getService( ClassLoaderService.class );

		this.explicitEntityName = determineExplicitEntityName();
		this.customLoaderQueryName = determineCustomLoader();
		this.synchronizedTableNames = determineSynchronizedTableNames();
		this.batchSize = determineBatchSize();

		this.customInsert = AnnotationParserHelper.processCustomSqlAnnotation(
				HibernateDotNames.SQL_INSERT,
				javaTypeDescriptor.getJandexClassInfo().annotations(),
				javaTypeDescriptor.getJandexClassInfo()
		);
		this.customUpdate = AnnotationParserHelper.processCustomSqlAnnotation(
				HibernateDotNames.SQL_UPDATE,
				javaTypeDescriptor.getJandexClassInfo().annotations(),
				javaTypeDescriptor.getJandexClassInfo()
		);
		this.customDelete = AnnotationParserHelper.processCustomSqlAnnotation(
				HibernateDotNames.SQL_DELETE,
				javaTypeDescriptor.getJandexClassInfo().annotations(),
				javaTypeDescriptor.getJandexClassInfo()
		);

		// dynamic insert (see HHH-6397)
		final AnnotationInstance dynamicInsertAnnotation = JandexHelper.getSingleAnnotation(
				javaTypeDescriptor.getJandexClassInfo(),
				HibernateDotNames.DYNAMIC_INSERT
		);
		if ( dynamicInsertAnnotation != null ) {
			this.isDynamicInsert = JandexHelper.getValue(
					dynamicInsertAnnotation,
					"value",
					Boolean.class,
					classLoaderService
			);
		}
		else {
			this.isDynamicInsert = false;
		}

		// dynamic update (see HHH-6398)
		final AnnotationInstance dynamicUpdateAnnotation = JandexHelper.getSingleAnnotation(
				javaTypeDescriptor.getJandexClassInfo(),
				HibernateDotNames.DYNAMIC_UPDATE
		);
		if ( dynamicUpdateAnnotation != null ) {
			this.isDynamicUpdate = JandexHelper.getValue(
					dynamicUpdateAnnotation,
					"value",
					Boolean.class,
					classLoaderService
			);
		}
		else {
			this.isDynamicUpdate = false;
		}


		// select-before-update (see HHH-6399)
		final AnnotationInstance selectBeforeUpdateAnnotation = JandexHelper.getSingleAnnotation(
				javaTypeDescriptor.getJandexClassInfo(),
				HibernateDotNames.SELECT_BEFORE_UPDATE
		);
		if ( selectBeforeUpdateAnnotation != null ) {
			this.isSelectBeforeUpdate = JandexHelper.getValue(
					selectBeforeUpdateAnnotation,
					"value",
					Boolean.class,
					classLoaderService
			);
		}
		else {
			this.isSelectBeforeUpdate = false;
		}

		// Custom persister
		String entityPersisterClass = null;
		final AnnotationInstance persisterAnnotation = JandexHelper.getSingleAnnotation(
				javaTypeDescriptor.getJandexClassInfo(),
				HibernateDotNames.PERSISTER,
				ClassInfo.class
		);
		if ( persisterAnnotation != null && persisterAnnotation.value( "impl" ) != null ) {
			entityPersisterClass = persisterAnnotation.value( "impl" ).asString();
		}
		this.customPersister = entityPersisterClass;

		// Proxy generation
		final AnnotationInstance hibernateProxyAnnotation = JandexHelper.getSingleAnnotation(
				javaTypeDescriptor.getJandexClassInfo(),
				HibernateDotNames.PROXY
		);
		if ( hibernateProxyAnnotation != null ) {
			this.isLazy = hibernateProxyAnnotation.value( "lazy" ) == null
					|| hibernateProxyAnnotation.value( "lazy" ).asBoolean();
			if ( this.isLazy ) {
				final AnnotationValue proxyClassValue = hibernateProxyAnnotation.value( "proxyClass" );
				this.proxy = proxyClassValue == null ? getName() : proxyClassValue.asString();
			}
			else {
				this.proxy = null;
			}
		}
		else {
			// defaults are that it is lazy and that the class itself is the proxy class
			this.isLazy = true;
			this.proxy = getName();
		}


		final AnnotationInstance discriminatorValueAnnotation = getJavaTypeDescriptor().findTypeAnnotation(
				JPADotNames.DISCRIMINATOR_VALUE
		);
		if ( discriminatorValueAnnotation != null ) {
			this.discriminatorMatchValue = discriminatorValueAnnotation.value().asString();
		}
		else {
			this.discriminatorMatchValue = null;
		}
		
		// TODO: bind JPA @ForeignKey?
		foreignKeyDelegate = new ForeignKeyDelegate();


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// todo : which (if any) of these to keep?
		this.joinedSubclassPrimaryKeyJoinColumnSources = determinePrimaryKeyJoinColumns();
		this.onDeleteAction = determineOnDeleteAction();
	}


	/**
	 * This form is intended for construction of Entity subclasses.
	 */
	public EntityTypeMetadata(
			ClassDescriptor javaTypeDescriptor,
			IdentifiableTypeMetadata superType,
			AccessType defaultAccessType,
			AnnotationBindingContext bindingContext) {
		super( javaTypeDescriptor, superType, defaultAccessType, bindingContext );
		
		this.classLoaderService = bindingContext.getServiceRegistry().getService( ClassLoaderService.class );

		this.explicitEntityName = determineExplicitEntityName();
		this.customLoaderQueryName = determineCustomLoader();
		this.synchronizedTableNames = determineSynchronizedTableNames();
		this.batchSize = determineBatchSize();

		this.customInsert = AnnotationParserHelper.processCustomSqlAnnotation(
				HibernateDotNames.SQL_INSERT,
				javaTypeDescriptor.getJandexClassInfo().annotations(),
				javaTypeDescriptor.getJandexClassInfo()
		);
		this.customUpdate = AnnotationParserHelper.processCustomSqlAnnotation(
				HibernateDotNames.SQL_UPDATE,
				javaTypeDescriptor.getJandexClassInfo().annotations(),
				javaTypeDescriptor.getJandexClassInfo()
		);
		this.customDelete = AnnotationParserHelper.processCustomSqlAnnotation(
				HibernateDotNames.SQL_DELETE,
				javaTypeDescriptor.getJandexClassInfo().annotations(),
				javaTypeDescriptor.getJandexClassInfo()
		);

		// dynamic insert (see HHH-6397)
		final AnnotationInstance dynamicInsertAnnotation = JandexHelper.getSingleAnnotation(
				javaTypeDescriptor.getJandexClassInfo(),
				HibernateDotNames.DYNAMIC_INSERT
		);
		if ( dynamicInsertAnnotation != null ) {
			this.isDynamicInsert = JandexHelper.getValue(
					dynamicInsertAnnotation,
					"value",
					Boolean.class,
					classLoaderService
			);
		}
		else {
			this.isDynamicInsert = false;
		}

		// dynamic update (see HHH-6398)
		final AnnotationInstance dynamicUpdateAnnotation = JandexHelper.getSingleAnnotation(
				javaTypeDescriptor.getJandexClassInfo(),
				HibernateDotNames.DYNAMIC_UPDATE
		);
		if ( dynamicUpdateAnnotation != null ) {
			this.isDynamicUpdate = JandexHelper.getValue(
					dynamicUpdateAnnotation,
					"value",
					Boolean.class,
					classLoaderService
			);
		}
		else {
			this.isDynamicUpdate = false;
		}


		// select-before-update (see HHH-6399)
		final AnnotationInstance selectBeforeUpdateAnnotation = JandexHelper.getSingleAnnotation(
				javaTypeDescriptor.getJandexClassInfo(),
				HibernateDotNames.SELECT_BEFORE_UPDATE
		);
		if ( selectBeforeUpdateAnnotation != null ) {
			this.isSelectBeforeUpdate = JandexHelper.getValue(
					selectBeforeUpdateAnnotation,
					"value",
					Boolean.class,
					classLoaderService
			);
		}
		else {
			this.isSelectBeforeUpdate = false;
		}

		// Custom persister
		String entityPersisterClass = null;
		final AnnotationInstance persisterAnnotation = JandexHelper.getSingleAnnotation(
				javaTypeDescriptor.getJandexClassInfo(),
				HibernateDotNames.PERSISTER,
				ClassInfo.class
		);
		if ( persisterAnnotation != null && persisterAnnotation.value( "impl" ) != null ) {
			entityPersisterClass = persisterAnnotation.value( "impl" ).asString();
		}
		this.customPersister = entityPersisterClass;

		// Proxy generation
		final AnnotationInstance hibernateProxyAnnotation = JandexHelper.getSingleAnnotation(
				javaTypeDescriptor.getJandexClassInfo(),
				HibernateDotNames.PROXY
		);
		if ( hibernateProxyAnnotation != null ) {
			this.isLazy = hibernateProxyAnnotation.value( "lazy" ) == null
					|| hibernateProxyAnnotation.value( "lazy" ).asBoolean();
			if ( this.isLazy ) {
				final AnnotationValue proxyClassValue = hibernateProxyAnnotation.value( "proxyClass" );
				this.proxy = proxyClassValue == null ? getName() : proxyClassValue.asString();
			}
			else {
				this.proxy = null;
			}
		}
		else {
			// defaults are that it is lazy and that the class itself is the proxy class
			this.isLazy = true;
			this.proxy = getName();
		}


		final AnnotationInstance discriminatorValueAnnotation = getJavaTypeDescriptor().findTypeAnnotation(
				JPADotNames.DISCRIMINATOR_VALUE
		);
		if ( discriminatorValueAnnotation != null ) {
			this.discriminatorMatchValue = discriminatorValueAnnotation.value().asString();
		}
		else {
			this.discriminatorMatchValue = null;
		}
		
		// TODO: bind JPA @ForeignKey?
		foreignKeyDelegate = new ForeignKeyDelegate();


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// todo : which (if any) of these to keep?
		this.joinedSubclassPrimaryKeyJoinColumnSources = determinePrimaryKeyJoinColumns();
		this.onDeleteAction = determineOnDeleteAction();
	}

	private static AccessType determineAccessType(ClassDescriptor javaTypeDescriptor, AccessType defaultAccessType) {
		AccessType accessType = defaultAccessType;

		final AnnotationInstance localAccessAnnotation = javaTypeDescriptor.findLocalTypeAnnotation( JPADotNames.ACCESS );
		if ( localAccessAnnotation != null ) {
			accessType = AccessType.valueOf( localAccessAnnotation.value().asEnum() );
		}

		return accessType;
	}

	private String determineExplicitEntityName() {
		final AnnotationInstance jpaEntityAnnotation = getJavaTypeDescriptor().findLocalTypeAnnotation( ENTITY );
		if ( jpaEntityAnnotation == null ) {
			// can this really ever be true here?!
			return null;
		}

		final AnnotationValue nameValue = jpaEntityAnnotation.value( "name" );
		if ( nameValue == null ) {
			return null;
		}

		return StringHelper.nullIfEmpty( nameValue.asString() );
	}

	private String determineCustomLoader() {
		String customLoader = null;
		// Custom sql loader
		final AnnotationInstance sqlLoaderAnnotation = getJavaTypeDescriptor().findTypeAnnotation(
				HibernateDotNames.LOADER
		);
		if ( sqlLoaderAnnotation != null && sqlLoaderAnnotation.target() instanceof ClassInfo) {
			customLoader = sqlLoaderAnnotation.value( "namedQuery" ).asString();
		}
		return customLoader;
	}

	private String[] determineSynchronizedTableNames() {
		final AnnotationInstance synchronizeAnnotation = getJavaTypeDescriptor().findTypeAnnotation(
				HibernateDotNames.SYNCHRONIZE
		);
		if ( synchronizeAnnotation != null ) {
			return synchronizeAnnotation.value().asStringArray();
		}
		else {
			return StringHelper.EMPTY_STRINGS;
		}
	}

	private int determineBatchSize() {
		final AnnotationInstance batchSizeAnnotation = getJavaTypeDescriptor().findTypeAnnotation(
				HibernateDotNames.BATCH_SIZE
		);
		return batchSizeAnnotation == null ? -1 : batchSizeAnnotation.value( "size" ).asInt();
	}

	private OnDeleteAction determineOnDeleteAction() {
		final AnnotationInstance onDeleteAnnotation = getJavaTypeDescriptor().findTypeAnnotation(
				HibernateDotNames.ON_DELETE
		);
		if ( onDeleteAnnotation != null ) {
			return JandexHelper.getEnumValue(
					onDeleteAnnotation,
					"action",
					OnDeleteAction.class,
					classLoaderService
			);
		}
		return null;
	}


	public String getExplicitEntityName() {
		return explicitEntityName;
	}

	public String getEntityName() {
		return getJavaTypeDescriptor().getName().local();
	}

	public boolean isDynamicInsert() {
		return isDynamicInsert;
	}

	public boolean isDynamicUpdate() {
		return isDynamicUpdate;
	}

	public boolean isSelectBeforeUpdate() {
		return isSelectBeforeUpdate;
	}

	public String getCustomLoaderQueryName() {
		return customLoaderQueryName;
	}

	public CustomSQL getCustomInsert() {
		return customInsert;
	}

	public CustomSQL getCustomUpdate() {
		return customUpdate;
	}

	public CustomSQL getCustomDelete() {
		return customDelete;
	}

	public String[] getSynchronizedTableNames() {
		return synchronizedTableNames;
	}

	public List<PrimaryKeyJoinColumn> getJoinedSubclassPrimaryKeyJoinColumnSources() {
		return joinedSubclassPrimaryKeyJoinColumnSources;
	}

	public String getCustomPersister() {
		return customPersister;
	}

	public boolean isLazy() {
		return isLazy;
	}

	public String getProxy() {
		return proxy;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public boolean isEntityRoot() {
		return getSuperType() == null;
	}

	public String getDiscriminatorMatchValue() {
		return discriminatorMatchValue;
	}


	public String getInverseForeignKeyName() {
		return foreignKeyDelegate.getInverseForeignKeyName();
	}
	public String getExplicitForeignKeyName(){
		return foreignKeyDelegate.getExplicitForeignKeyName();
	}
	public boolean createForeignKeyConstraint(){
		return foreignKeyDelegate.createForeignKeyConstraint();
 	}

	public OnDeleteAction getOnDeleteAction() {
		return onDeleteAction;
	}


	protected List<PrimaryKeyJoinColumn> determinePrimaryKeyJoinColumns() {
		final AnnotationInstance primaryKeyJoinColumns = getJavaTypeDescriptor().findLocalTypeAnnotation(
				JPADotNames.PRIMARY_KEY_JOIN_COLUMNS
		);
		final AnnotationInstance primaryKeyJoinColumn = getJavaTypeDescriptor().findLocalTypeAnnotation(
				JPADotNames.PRIMARY_KEY_JOIN_COLUMN
		);

		final List<PrimaryKeyJoinColumn> results;
		if ( primaryKeyJoinColumns != null ) {
			AnnotationInstance[] values = primaryKeyJoinColumns.value().asNestedArray();
			results = new ArrayList<PrimaryKeyJoinColumn>( values.length );
			for ( final AnnotationInstance annotationInstance : values ) {
				results.add( new PrimaryKeyJoinColumn( annotationInstance ) );
			}
		}
		else if ( primaryKeyJoinColumn != null ) {
			results = new ArrayList<PrimaryKeyJoinColumn>( 1 );
			results.add( new PrimaryKeyJoinColumn( primaryKeyJoinColumn ) );
		}
		else {
			results = null;
		}
		return results;
	}

	public boolean hasMultiTenancySourceInformation() {
		return getJavaTypeDescriptor().findTypeAnnotation( HibernateDotNames.MULTI_TENANT ) != null
				|| getJavaTypeDescriptor().findTypeAnnotation( HibernateDotNames.TENANT_COLUMN ) != null
				|| getJavaTypeDescriptor().findTypeAnnotation( HibernateDotNames.TENANT_FORMULA ) != null;
	}

	public boolean containsDiscriminator() {
		return getJavaTypeDescriptor().findTypeAnnotation( JPADotNames.DISCRIMINATOR_COLUMN ) != null
				|| getJavaTypeDescriptor().findTypeAnnotation( HibernateDotNames.DISCRIMINATOR_FORMULA ) != null;
	}
}
