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
package org.hibernate.metamodel.binder.source.annotations;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import org.hibernate.EntityMode;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.metamodel.binder.Origin;
import org.hibernate.metamodel.binder.source.EntityDescriptor;
import org.hibernate.metamodel.binder.source.MetaAttributeContext;
import org.hibernate.metamodel.binder.source.UnifiedDescriptorObject;
import org.hibernate.metamodel.binder.source.annotations.entity.ConfiguredClass;
import org.hibernate.metamodel.binding.CustomSQL;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.entity.EntityTuplizer;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractEntityDescriptorImpl implements EntityDescriptor {
	private final ConfiguredClass configuredClass;
	@SuppressWarnings( {"FieldCanBeLocal", "UnusedDeclaration"}) // for now this is not used.
	private final AnnotationsBindingContext bindingContext;

	private final String jpaEntityName;

	private final String superEntityName;
	private final InheritanceType inheritanceType;

	private final boolean lazy;
	private final String proxyInterfaceName;

	private final Class<EntityPersister> entityPersisterClass;
	private final Class<EntityTuplizer> tuplizerClass;

	private final int batchSize;

	private final boolean dynamicUpdate;
	private final boolean dynamicInsert;

	private final boolean selectBeforeUpdate;

	private final String customLoaderName;
	private final CustomSQL customInsert;
	private final CustomSQL customUpdate;
	private final CustomSQL customDelete;

	private final Set<String> synchronizedTableNames;

	public AbstractEntityDescriptorImpl(
			ConfiguredClass configuredClass,
			String superEntityName,
			InheritanceType inheritanceType,
			AnnotationsBindingContext bindingContext) {
		this.configuredClass = configuredClass;
		this.superEntityName = superEntityName;
		this.inheritanceType = inheritanceType;
		this.bindingContext = bindingContext;

		final AnnotationInstance jpaEntityAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), JPADotNames.ENTITY
		);

		final AnnotationValue explicitJpaEntityName = jpaEntityAnnotation.value( "name" );
		if ( explicitJpaEntityName == null ) {
			jpaEntityName = configuredClass.getName();
		}
		else {
			jpaEntityName = explicitJpaEntityName.asString();
		}

		final AnnotationInstance hibernateEntityAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.ENTITY
		);

		this.dynamicInsert = hibernateEntityAnnotation != null
				&& hibernateEntityAnnotation.value( "dynamicInsert" ) != null
				&& hibernateEntityAnnotation.value( "dynamicInsert" ).asBoolean();

		this.dynamicUpdate = hibernateEntityAnnotation != null
				&& hibernateEntityAnnotation.value( "dynamicUpdate" ) != null
				&& hibernateEntityAnnotation.value( "dynamicUpdate" ).asBoolean();

		this.selectBeforeUpdate = hibernateEntityAnnotation != null
				&& hibernateEntityAnnotation.value( "selectBeforeUpdate" ) != null
				&& hibernateEntityAnnotation.value( "selectBeforeUpdate" ).asBoolean();

		final AnnotationInstance sqlLoaderAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.LOADER
		);
		this.customLoaderName = sqlLoaderAnnotation == null
				? null
				: sqlLoaderAnnotation.value( "namedQuery" ).asString();
		final AnnotationInstance sqlInsertAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.SQL_INSERT
		);
		this.customInsert = createCustomSQL( sqlInsertAnnotation );
		final AnnotationInstance sqlUpdateAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.SQL_UPDATE
		);
		this.customUpdate = createCustomSQL( sqlUpdateAnnotation );
		final AnnotationInstance sqlDeleteAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.SQL_DELETE
		);
		this.customDelete = createCustomSQL( sqlDeleteAnnotation );

		final AnnotationInstance batchSizeAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.BATCH_SIZE
		);
		this.batchSize = batchSizeAnnotation == null
				? -1
				: batchSizeAnnotation.value( "size" ).asInt();

		final AnnotationInstance hibernateProxyAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.PROXY
		);
		if ( hibernateProxyAnnotation != null ) {
			this.lazy = hibernateProxyAnnotation.value( "lazy" ) == null
					|| hibernateProxyAnnotation.value( "lazy" ).asBoolean();
			final AnnotationValue proxyClassValue = hibernateProxyAnnotation.value( "proxyClass" );
			if ( proxyClassValue == null ) {
				this.proxyInterfaceName = null;
			}
			else {
				this.proxyInterfaceName = bindingContext.locateClassByName( proxyClassValue.asString() ).getName();
			}
		}
		else {
			this.lazy = true;
			this.proxyInterfaceName = configuredClass.getName();
		}

		final AnnotationInstance persisterAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.PERSISTER
		);
		if ( persisterAnnotation == null || persisterAnnotation.value( "impl" ) == null ) {
			if ( hibernateEntityAnnotation != null && hibernateEntityAnnotation.value( "persister" ) != null ) {
				this.entityPersisterClass = bindingContext.locateClassByName( hibernateEntityAnnotation.value( "persister" ).asString() );
			}
			else {
				this.entityPersisterClass = null;
			}
		}
		else {
			if ( hibernateEntityAnnotation != null && hibernateEntityAnnotation.value( "persister" ) != null ) {
				// todo : error?
			}
			this.entityPersisterClass = bindingContext.locateClassByName( persisterAnnotation.value( "impl" ).asString() );
		}

		final AnnotationInstance pojoTuplizerAnnotation = locatePojoTuplizerAnnotation();
		if ( pojoTuplizerAnnotation == null ) {
			tuplizerClass = null;
		}
		else {
			tuplizerClass = bindingContext.locateClassByName( pojoTuplizerAnnotation.value( "impl" ).asString() );
		}

		final AnnotationInstance synchronizeAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.SYNCHRONIZE
		);
		if ( synchronizeAnnotation != null ) {
			synchronizedTableNames = new HashSet<String>();
			final String[] tableNames = synchronizeAnnotation.value().asStringArray();
			synchronizedTableNames.addAll( Arrays.asList( tableNames ) );
		}
		else {
			synchronizedTableNames = java.util.Collections.emptySet();
		}
	}

	private CustomSQL createCustomSQL(AnnotationInstance customSQLAnnotation) {
		if ( customSQLAnnotation == null ) {
			return null;
		}

		String sql = customSQLAnnotation.value( "sql" ).asString();
		boolean isCallable = false;
		AnnotationValue callableValue = customSQLAnnotation.value( "callable" );
		if ( callableValue != null ) {
			isCallable = callableValue.asBoolean();
		}

		ResultCheckStyle checkStyle = ResultCheckStyle.NONE;
		AnnotationValue checkStyleValue = customSQLAnnotation.value( "check" );
		if ( checkStyleValue != null ) {
			checkStyle = Enum.valueOf( ResultCheckStyle.class, checkStyleValue.asEnum() );
		}

		return new CustomSQL(
				sql,
				isCallable,
				Enum.valueOf( ExecuteUpdateResultCheckStyle.class, checkStyle.toString() )
		);
	}

	private AnnotationInstance locatePojoTuplizerAnnotation() {
		final AnnotationInstance tuplizersAnnotation = JandexHelper.getSingleAnnotation(
				configuredClass.getClassInfo(), HibernateDotNames.SYNCHRONIZE
		);
		if ( tuplizersAnnotation == null ) {
			return null;
		}

		for ( AnnotationInstance tuplizerAnnotation : JandexHelper.getValueAsArray(tuplizersAnnotation, "value" ) ) {
			if ( EntityMode.valueOf( tuplizerAnnotation.value( "entityModeType" ).asEnum() ) == EntityMode.POJO ) {
				return tuplizerAnnotation;
			}
		}

		return null;
	}

	@Override
	public String getClassName() {
		return configuredClass.getName();
	}

	@Override
	public String getEntityName() {
		return getClassName();
	}

	@Override
	public String getJpaEntityName() {
		return jpaEntityName;
	}

	@Override
	public EntityMode getEntityMode() {
		return EntityMode.POJO;
	}

	@Override
	public String getProxyInterfaceName() {
		return proxyInterfaceName;
	}

	@Override
	public Class<EntityPersister> getCustomEntityPersisterClass() {
		return entityPersisterClass;
	}

	@Override
	public Class<EntityTuplizer> getCustomEntityTuplizerClass() {
		return tuplizerClass;
	}

	@Override
	public String getSuperEntityName() {
		return superEntityName;
	}

	@Override
	public InheritanceType getEntityInheritanceType() {
		return inheritanceType;
	}

	@Override
	public MetaAttributeContext getMetaAttributeContext() {
		return null;
	}

	@Override
	public boolean isLazy() {
		return lazy;
	}

	@Override
	public boolean isDynamicUpdate() {
		return dynamicUpdate;
	}

	@Override
	public boolean isDynamicInsert() {
		return dynamicInsert;
	}

	@Override
	public int getBatchSize() {
		return batchSize;
	}

	@Override
	public boolean isSelectBeforeUpdate() {
		return selectBeforeUpdate;
	}

	@Override
	public Boolean isAbstract() {
		return false;
	}

	@Override
	public String getCustomLoaderName() {
		return customLoaderName;
	}

	@Override
	public CustomSQL getCustomInsert() {
		return customInsert;
	}

	@Override
	public CustomSQL getCustomUpdate() {
		return customUpdate;
	}

	@Override
	public CustomSQL getCustomDelete() {
		return customDelete;
	}

	@Override
	public Set<String> getSynchronizedTableNames() {
		return synchronizedTableNames;
	}

	@Override
	public UnifiedDescriptorObject getContainingDescriptor() {
		return null;
	}

	@Override
	public Origin getOrigin() {
//		return bindingContext.getOrigin();
		return null;
	}
}
