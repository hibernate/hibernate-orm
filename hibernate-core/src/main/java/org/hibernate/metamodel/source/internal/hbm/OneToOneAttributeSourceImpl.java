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
package org.hibernate.metamodel.source.internal.hbm;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbColumnElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbOneToOneElement;
import org.hibernate.metamodel.source.spi.AttributeSourceContainer;
import org.hibernate.metamodel.source.spi.RelationalValueSource;
import org.hibernate.metamodel.source.spi.ToolingHintSource;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.NaturalIdMutability;
import org.hibernate.metamodel.spi.SingularAttributeNature;
import org.hibernate.type.ForeignKeyDirection;

/**
 * Implementation for {@code <one-to-one/>} mappings
 *
 * @author Gail Badner
 * @author Steve Ebersole
 */
class OneToOneAttributeSourceImpl extends AbstractToOneAttributeSourceImpl {
	private final JaxbOneToOneElement oneToOneElement;
	private final HibernateTypeSourceImpl typeSource;

	private final String containingTableName;
	private final List<RelationalValueSource> valueSources;

	private final AttributeRole attributeRole;
	private final AttributePath attributePath;

	OneToOneAttributeSourceImpl(
			MappingDocument sourceMappingDocument,
			AttributeSourceContainer container,
			final JaxbOneToOneElement oneToOneElement,
			final String logicalTableName,
			NaturalIdMutability naturalIdMutability) {
		super( sourceMappingDocument, naturalIdMutability, oneToOneElement.getPropertyRef() );
		this.oneToOneElement = oneToOneElement;

		final String referencedClassName = oneToOneElement.getClazz();
		JavaTypeDescriptor referencedClass = null;
		if ( StringHelper.isNotEmpty( referencedClassName ) ) {
			referencedClass = bindingContext().getJavaTypeDescriptorRepository().getType(
					bindingContext().getJavaTypeDescriptorRepository().buildName(
							bindingContext().qualifyClassName( oneToOneElement.getClazz() )
					)
			);
		}
		this.typeSource = new HibernateTypeSourceImpl( referencedClass );

		this.containingTableName = logicalTableName;
		this.valueSources = Helper.buildValueSources(
				sourceMappingDocument(),
				new Helper.ValueSourcesAdapter() {
					@Override
					public String getColumnAttribute() {
						// Not applicable to one-to-one
						return null;
					}

					@Override
					public String getFormulaAttribute() {
						return oneToOneElement.getFormulaAttribute();
					}

					@Override
					public List<JaxbColumnElement> getColumn() {
						// Not applicable to one-to-one
						return null;
					}

					@Override
					public List<String> getFormula() {
						return oneToOneElement.getFormula();
					}

					@Override
					public String getContainingTableName() {
						return logicalTableName;
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

		this.attributeRole = container.getAttributeRoleBase().append( oneToOneElement.getName() );
		this.attributePath = container.getAttributePathBase().append( oneToOneElement.getName() );
	}

	@Override
	public String getName() {
		return oneToOneElement.getName();
	}

	@Override
	public AttributePath getAttributePath() {
		return attributePath;
	}

	@Override
	public AttributeRole getAttributeRole() {
		return attributeRole;
	}

	@Override
	public HibernateTypeSourceImpl getTypeInformation() {
		return typeSource;
	}

	@Override
	public String getPropertyAccessorName() {
		return oneToOneElement.getAccess();
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return false;
	}

	@Override
	public Set<CascadeStyle> getCascadeStyles() {
		return Helper.interpretCascadeStyles( oneToOneElement.getCascade(), bindingContext() );
	}

	@Override
	protected String getFetchSelectionString() {
		return oneToOneElement.getFetch()!=null? oneToOneElement.getFetch().value() : null;
	}

	@Override
	protected String getLazySelectionString() {
		return oneToOneElement.getLazy()!=null? oneToOneElement.getLazy().value() : null;
	}

	@Override
	protected String getOuterJoinSelectionString() {
		return oneToOneElement.getOuterJoin()!=null? oneToOneElement.getOuterJoin().value() : null;
	}

	@Override
	public SingularAttributeNature getSingularAttributeNature() {
		return SingularAttributeNature.ONE_TO_ONE;
	}

	@Override
	public boolean isVirtualAttribute() {
		return false;
	}

	@Override
	protected boolean requiresImmediateFetch() {
		return !oneToOneElement.isConstrained();
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
		return containingTableName;
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		return valueSources;
	}

	@Override
	public Collection<? extends ToolingHintSource> getToolingHintSources() {
		return oneToOneElement.getMeta();
	}

	@Override
	public String getReferencedEntityName() {
		return oneToOneElement.getClazz() != null
				? bindingContext().qualifyClassName( oneToOneElement.getClazz() )
				: oneToOneElement.getEntityName();
	}

	@Override
	public boolean isUnique() {
		return true;
	}

	@Override
	public String getExplicitForeignKeyName() {
		return oneToOneElement.getForeignKey();
	}

	@Override
	public boolean isCascadeDeleteEnabled() {
		return false;
	}

	@Override
	public ForeignKeyDirection getForeignKeyDirection() {
		return oneToOneElement.isConstrained()  ? ForeignKeyDirection.FROM_PARENT : ForeignKeyDirection.TO_PARENT;
	}
}
