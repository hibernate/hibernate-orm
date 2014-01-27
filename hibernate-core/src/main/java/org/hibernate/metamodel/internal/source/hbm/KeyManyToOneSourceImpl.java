/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jaxb.spi.hbm.JaxbColumnElement;
import org.hibernate.jaxb.spi.hbm.JaxbKeyManyToOneElement;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;
import org.hibernate.metamodel.spi.source.SingularAttributeSource;
import org.hibernate.type.ForeignKeyDirection;

/**
 * Implementation for {@code <key-many-to-one/>} mappings
 *
 * @author Gail Badner
 */
class KeyManyToOneSourceImpl
		extends AbstractToOneAttributeSourceImpl
		implements SingularAttributeSource {
	private final JaxbKeyManyToOneElement keyManyToOneElement;
	private final List<RelationalValueSource> valueSources;

	public KeyManyToOneSourceImpl(
			MappingDocument mappingDocument,
			final JaxbKeyManyToOneElement keyManyToOneElement,
			final SingularAttributeBinding.NaturalIdMutability naturalIdMutability) {
		super( mappingDocument, naturalIdMutability, null );
		this.keyManyToOneElement = keyManyToOneElement;
		this.valueSources = Helper.buildValueSources(
				sourceMappingDocument(),
				new Helper.ValueSourcesAdapter() {
					@Override
					public String getColumnAttribute() {
						return keyManyToOneElement.getColumnAttribute();
					}

					@Override
					public String getFormulaAttribute() {
						return null;
					}

					@Override
					public List<JaxbColumnElement> getColumn() {
						return keyManyToOneElement.getColumn();
					}

					@Override
					public List<String> getFormula() {
						return Collections.emptyList();
					}

					@Override
					public String getContainingTableName() {
						return null;
					}

					@Override
					public boolean isIncludedInInsertByDefault() {
						return true;
					}

					@Override
					public boolean isIncludedInUpdateByDefault() {
						return false;
					}
				}
		);
	}

	@Override
	public String getName() {
		return keyManyToOneElement.getName();
	}

	@Override
	public String getPropertyAccessorName() {
		return keyManyToOneElement.getAccess();
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return false;
	}

	@Override
	public Nature getNature() {
		return Nature.MANY_TO_ONE;
	}

	@Override
	public boolean isVirtualAttribute() {
		return false;
	}

	@Override
	protected boolean requiresImmediateFetch() {
		return false;
	}

	@Override
	protected String getFetchSelectionString() {
		return null;
	}

	@Override
	protected String getLazySelectionString() {
		return keyManyToOneElement.getLazy() != null ?
				keyManyToOneElement.getLazy().value() :
				null;
	}

	@Override
	protected String getOuterJoinSelectionString() {
		return null;
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return true;
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return true;
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return false;
	}

	@Override
	public String getContainingTableName() {
		return null;
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		return valueSources;
	}

	@Override
	public Iterable<? extends MetaAttributeSource> getMetaAttributeSources() {
		return keyManyToOneElement.getMeta();
	}

	@Override
	public String getReferencedEntityName() {
		return keyManyToOneElement.getEntityName();
	}

	@Override
	public boolean isUnique() {
		return false;
	}

	@Override
	public ForeignKeyDirection getForeignKeyDirection() {
		return ForeignKeyDirection.TO_PARENT;
	}

	@Override
	public Set<CascadeStyle> getCascadeStyles() {
		return Collections.singleton( CascadeStyles.NONE );
	}

	@Override
	public String getExplicitForeignKeyName() {
		return keyManyToOneElement.getForeignKey();
	}
}
