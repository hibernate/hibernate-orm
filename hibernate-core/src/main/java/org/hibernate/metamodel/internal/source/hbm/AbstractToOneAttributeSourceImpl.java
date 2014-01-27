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
package org.hibernate.metamodel.internal.source.hbm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.cfg.NamingStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.internal.Binder;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.CompositeAttributeBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.AttributeSourceResolutionContext;
import org.hibernate.metamodel.spi.source.HibernateTypeSource;
import org.hibernate.metamodel.spi.source.MappedByAssociationSource;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.metamodel.spi.source.ToOneAttributeSource;

/**
 * @author Gail Badner
 */
public abstract class AbstractToOneAttributeSourceImpl extends AbstractHbmSourceNode implements ToOneAttributeSource{
	private final SingularAttributeBinding.NaturalIdMutability naturalIdMutability;
	private final String propertyRef;
	private final Set<MappedByAssociationSource> ownedAssociationSources = new HashSet<MappedByAssociationSource>(  );

	AbstractToOneAttributeSourceImpl(
			MappingDocument sourceMappingDocument,
			SingularAttributeBinding.NaturalIdMutability naturalIdMutability,
			String propertyRef) {
		super( sourceMappingDocument );
		this.naturalIdMutability = naturalIdMutability;
		this.propertyRef = propertyRef;

	}
	@Override
	public HibernateTypeSource getTypeInformation() {
		return Helper.TO_ONE_ATTRIBUTE_TYPE_SOURCE;
	}

	@Override
	public SingularAttributeBinding.NaturalIdMutability getNaturalIdMutability() {
		return naturalIdMutability;
	}

	@Override
	public boolean isSingular() {
		return true;
	}

	@Override
	public boolean isVirtualAttribute() {
		return false;
	}

	@Override
	public PropertyGeneration getGeneration() {
		return PropertyGeneration.NEVER;
	}

	@Override
	public boolean isNotFoundAnException() {
		return true;
	}

	@Override
	public boolean isLazy() {
		return getFetchTiming() != FetchTiming.IMMEDIATE;
	}

	protected abstract boolean requiresImmediateFetch();
	protected abstract String getFetchSelectionString();
	protected abstract String getLazySelectionString();
	protected abstract String getOuterJoinSelectionString();

	@Override
	public boolean isUnWrapProxy() {
		final String lazySelection = getLazySelectionString();
		return lazySelection != null && lazySelection.equals( "no-proxy" );
	}

	@Override
	public FetchTiming getFetchTiming() {
		final String lazySelection = getLazySelectionString();

		if ( lazySelection == null ) {
			if ( requiresImmediateFetch() ) {
				return FetchTiming.IMMEDIATE;
			}
			else if ( "join".equals( getFetchSelectionString() ) || "true".equals( getOuterJoinSelectionString() ) ) {
				return FetchTiming.IMMEDIATE;
			}
			else if ( "false".equals( getOuterJoinSelectionString() ) ) {
				return FetchTiming.DELAYED;
			}
			else {
				return bindingContext().getMappingDefaults().areAssociationsLazy()
						? FetchTiming.DELAYED
						: FetchTiming.IMMEDIATE;
			}
		}
		else  if ( "extra".equals( lazySelection ) ) {
			// TODO: don't think "extra" is valid here
			return FetchTiming.EXTRA_LAZY;
		}
		else if ( "true".equals( lazySelection ) || "proxy".equals( lazySelection ) || "no-proxy".equals( lazySelection ) ) {
			return FetchTiming.DELAYED;
		}
		else if ( "false".equals( lazySelection ) ) {
			return FetchTiming.IMMEDIATE;
		}

		throw new MappingException(
				String.format(
						"Unexpected lazy selection [%s] on '%s'",
						lazySelection,
						getName()
				),
				origin()
		);
	}

	@Override
	public FetchStyle getFetchStyle() {
		// todo : handle batch fetches?

		if ( getFetchSelectionString() == null ) {
			if ( requiresImmediateFetch() ) {
				return FetchStyle.JOIN;
			}
			else if ( getOuterJoinSelectionString() == null ) {
				return FetchStyle.SELECT;
			}
			else {
				if ( "auto".equals( getOuterJoinSelectionString() ) ) {
					return bindingContext().getMappingDefaults().areAssociationsLazy()
							? FetchStyle.SELECT
							: FetchStyle.JOIN;
				}
				else {
					return "true".equals( getOuterJoinSelectionString() ) ? FetchStyle.JOIN : FetchStyle.SELECT;
				}
			}
		}
		else {
			return "join".equals( getFetchSelectionString() ) ? FetchStyle.JOIN : FetchStyle.SELECT;
		}
	}

	@Override
	// TODO: change to return the default name for a single column
	public List<Binder.DefaultNamingStrategy> getDefaultNamingStrategies(
			final String entityName,
			final String tableName,
			final AttributeBinding referencedAttributeBinding) {
		if ( CompositeAttributeBinding.class.isInstance( referencedAttributeBinding ) ) {
			CompositeAttributeBinding compositeAttributeBinding = CompositeAttributeBinding.class.cast(
					referencedAttributeBinding
			);
			List<Binder.DefaultNamingStrategy> result = new ArrayList<Binder.DefaultNamingStrategy>();
			for ( final AttributeBinding attributeBinding : compositeAttributeBinding.attributeBindings() ) {
				result.addAll( getDefaultNamingStrategies( entityName, tableName, attributeBinding ) );
			}
			return result;
		}
		else {
			List<Binder.DefaultNamingStrategy> result = new ArrayList<Binder.DefaultNamingStrategy>( 1 );
			result.add(
					new Binder.DefaultNamingStrategy() {
						@Override
						public String defaultName(NamingStrategy namingStrategy) {
							return namingStrategy.propertyToColumnName( getName() );
						}
					}
			);
			return result;
		}

	}

	@Override
	public Set<MappedByAssociationSource> getOwnedAssociationSources() {
		return ownedAssociationSources;
	}

	@Override
	public void addMappedByAssociationSource(MappedByAssociationSource attributeSource) {
		ownedAssociationSources.add( attributeSource );
	}

	@Override
	public boolean isMappedBy() {
		// only applies to annotations
		return false;
	}

	@Override
	public AttributeSource getAttributeSource() {
		return this;
	}

	@Override
	public void resolveToOneAttributeSource(AttributeSourceResolutionContext context) {
		// nothing to do
	}

	@Override
	public JoinColumnResolutionDelegate getForeignKeyTargetColumnResolutionDelegate() {
		return propertyRef == null
				? null
				: new JoinColumnResolutionDelegateImpl( propertyRef );
	}

	public static class JoinColumnResolutionDelegateImpl implements JoinColumnResolutionDelegate {
		private final String propertyRef;

		public JoinColumnResolutionDelegateImpl(String propertyRef) {
			this.propertyRef = propertyRef;
		}

		@Override
		public String getReferencedAttributeName() {
			return propertyRef;
		}

		@Override
		public List<? extends Value> getJoinColumns(JoinColumnResolutionContext context) {
			return context.resolveRelationalValuesForAttribute( propertyRef );
		}

		@Override
		public TableSpecification getReferencedTable(JoinColumnResolutionContext context) {
			return context.resolveTableForAttribute( propertyRef );
		}
	}

}
