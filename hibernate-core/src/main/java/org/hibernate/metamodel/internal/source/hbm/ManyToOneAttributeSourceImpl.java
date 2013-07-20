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
package org.hibernate.metamodel.internal.source.hbm;

import java.util.List;
import java.util.Set;

import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.jaxb.spi.hbm.JaxbColumnElement;
import org.hibernate.jaxb.spi.hbm.JaxbManyToOneElement;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;
import org.hibernate.type.ForeignKeyDirection;

/**
 * Implementation for {@code <many-to-one/>} mappings
 *
 * @author Steve Ebersole
 */
class ManyToOneAttributeSourceImpl extends AbstractToOneAttributeSourceImpl {
	private final JaxbManyToOneElement manyToOneElement;
	private final String containingTableName;
	private final List<RelationalValueSource> valueSources;

	ManyToOneAttributeSourceImpl(
			MappingDocument sourceMappingDocument,
			final JaxbManyToOneElement manyToOneElement,
			final String logicalTableName,
			SingularAttributeBinding.NaturalIdMutability naturalIdMutability) {
		super( sourceMappingDocument, naturalIdMutability, manyToOneElement.getPropertyRef() );
		this.manyToOneElement = manyToOneElement;
		this.containingTableName = logicalTableName;
		this.valueSources = Helper.buildValueSources(
				sourceMappingDocument(),
				new Helper.ValueSourcesAdapter() {
					@Override
					public String getColumnAttribute() {
						return manyToOneElement.getColumnAttribute();
					}

					@Override
					public String getFormulaAttribute() {
						return manyToOneElement.getFormulaAttribute();
					}

					@Override
					public List<JaxbColumnElement> getColumn() {
						return manyToOneElement.getColumn();
					}

					@Override
					public List<String> getFormula() {
						return manyToOneElement.getFormula();
					}

					@Override
					public String getContainingTableName() {
						return logicalTableName;
					}

					@Override
					public boolean isIncludedInInsertByDefault() {
						return manyToOneElement.isInsert();
					}

					@Override
					public boolean isIncludedInUpdateByDefault() {
						return manyToOneElement.isUpdate();
					}
				}
		);
	}

	@Override
	public String getName() {
			return manyToOneElement.getName();
	}

	@Override
	public String getPropertyAccessorName() {
		return manyToOneElement.getAccess();
	}

	@Override
	public boolean isNotFoundAnException() {
		return manyToOneElement.getNotFound() == null || !"ignore".equals( manyToOneElement.getNotFound().value() );
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return manyToOneElement.isOptimisticLock();
	}

	@Override
	public Set<CascadeStyle> getCascadeStyles() {
		return Helper.interpretCascadeStyles( manyToOneElement.getCascade(), bindingContext() );
	}

	@Override
	protected boolean requiresImmediateFetch() {
		return false;
	}

	@Override
	protected String getFetchSelectionString() {
		return manyToOneElement.getFetch() != null ?
				manyToOneElement.getFetch().value() :
				null;
	}

	@Override
	protected String getLazySelectionString() {
		return manyToOneElement.getLazy() != null ?
				manyToOneElement.getLazy().value() :
				null;
	}

	@Override
	protected String getOuterJoinSelectionString() {
		return manyToOneElement.getOuterJoin() != null ?
				manyToOneElement.getOuterJoin().value() :
				null;
	}

	@Override
	public Nature getNature() {
		return Nature.MANY_TO_ONE;
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return manyToOneElement.isInsert();
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return manyToOneElement.isUpdate();
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return ! Helper.getValue( manyToOneElement.isNotNull(), false );
	}

	@Override
	public String getContainingTableName() {
		return containingTableName;
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		return valueSources;
	}

	@Override
	public Iterable<? extends MetaAttributeSource> getMetaAttributeSources() {
		return manyToOneElement.getMeta();
	}

	@Override
	public String getReferencedEntityName() {
		return manyToOneElement.getClazz() != null
				? bindingContext().qualifyClassName( manyToOneElement.getClazz() )
				: manyToOneElement.getEntityName();
	}

	@Override
	public boolean isUnique() {
		return manyToOneElement.isUnique();
	}

	@Override
	public String getExplicitForeignKeyName() {
		return manyToOneElement.getForeignKey();
	}

	@Override
	public ForeignKeyDirection getForeignKeyDirection() {
		return ForeignKeyDirection.TO_PARENT;
	}

}
