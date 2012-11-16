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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jaxb.spi.hbm.JaxbColumnElement;
import org.hibernate.jaxb.spi.hbm.JaxbManyToManyElement;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.metamodel.spi.source.ManyToManyPluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class ManyToManyPluralAttributeElementSourceImpl
		extends AbstractHbmSourceNode
		implements ManyToManyPluralAttributeElementSource {
	private final JaxbManyToManyElement manyToManyElement;
	private final Iterable<CascadeStyle> cascadeStyles;

	private final List<RelationalValueSource> valueSources;

	public ManyToManyPluralAttributeElementSourceImpl(
			MappingDocument mappingDocument,
			final JaxbManyToManyElement manyToManyElement,
			String cascadeString) {
		super( mappingDocument );
		this.manyToManyElement = manyToManyElement;
		this.cascadeStyles = Helper.interpretCascadeStyles( cascadeString, bindingContext() );

		this.valueSources = Helper.buildValueSources(
				sourceMappingDocument(),
				new Helper.ValueSourcesAdapter() {
					@Override
					public boolean isIncludedInInsertByDefault() {
						return true;
					}

					@Override
					public boolean isIncludedInUpdateByDefault() {
						return true;
					}

					@Override
					public String getColumnAttribute() {
						return manyToManyElement.getColumnAttribute();
					}

					@Override
					public String getFormulaAttribute() {
						return manyToManyElement.getFormulaAttribute();
					}

					@Override
					public List<JaxbColumnElement> getColumn() {
						return manyToManyElement.getColumn();
					}

					@Override
					public List<String> getFormula() {
						return manyToManyElement.getFormula();
					}
				}
		);
	}

	@Override
	public Nature getNature() {
		return Nature.MANY_TO_MANY;
	}

	@Override
	public String getReferencedEntityName() {
		return StringHelper.isNotEmpty( manyToManyElement.getEntityName() )
				? manyToManyElement.getEntityName()
				: bindingContext().qualifyClassName( manyToManyElement.getClazz() );
	}

	@Override
	public String getReferencedEntityAttributeName() {
		return manyToManyElement.getPropertyRef();
	}

	@Override
	// used by JPA instead of referenced entity attribute
	public Collection<String> getReferencedColumnNames() {
		return Collections.emptyList();
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		return valueSources;
	}

	@Override
	public boolean isNotFoundAnException() {
		return manyToManyElement.getNotFound() == null || !"ignore".equals( manyToManyElement.getNotFound().value() );
	}

	@Override
	public String getExplicitForeignKeyName() {
		return manyToManyElement.getForeignKey();
	}

	@Override
	public JoinColumnResolutionDelegate getForeignKeyTargetColumnResolutionDelegate() {
		return manyToManyElement.getPropertyRef() == null
				? null
				: new JoinColumnResolutionDelegateImpl();
	}

	@Override
	public boolean isUnique() {
		return manyToManyElement.isUnique();
	}

	@Override
	public String getOrderBy() {
		return manyToManyElement.getOrderBy();
	}

	@Override
	public String getWhere() {
		return manyToManyElement.getWhere();
	}

	@Override
	public Iterable<CascadeStyle> getCascadeStyles() {
		return cascadeStyles;
	}

	@Override
	public boolean fetchImmediately() {
		if ( manyToManyElement.getLazy() != null ) {
			if ( "false".equals( manyToManyElement.getLazy().value() ) ) {
				return true;
			}
		}

		if ( manyToManyElement.getOuterJoin() == null ) {
			return !bindingContext().getMappingDefaults().areAssociationsLazy();
		}
		else {
			final String value = manyToManyElement.getOuterJoin().value();
			if ( "auto".equals( value ) ) {
			}
			return "true".equals( value );
		}
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

	public class JoinColumnResolutionDelegateImpl implements JoinColumnResolutionDelegate {
		@Override
		public String getReferencedAttributeName() {
			return manyToManyElement.getPropertyRef();
		}

		@Override
		public List<Value> getJoinColumns(JoinColumnResolutionContext context) {
			return context.resolveRelationalValuesForAttribute( manyToManyElement.getPropertyRef() );
		}
	}

}
