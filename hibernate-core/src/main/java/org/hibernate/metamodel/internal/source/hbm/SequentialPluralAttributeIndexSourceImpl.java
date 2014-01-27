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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.cfg.NamingStrategy;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jaxb.spi.hbm.JaxbColumnElement;
import org.hibernate.jaxb.spi.hbm.JaxbIndexElement;
import org.hibernate.jaxb.spi.hbm.JaxbListIndexElement;
import org.hibernate.metamodel.internal.Binder;
import org.hibernate.metamodel.spi.binding.PluralAttributeIndexBinding;
import org.hibernate.metamodel.spi.source.HibernateTypeSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;
import org.hibernate.metamodel.spi.source.SequentialPluralAttributeIndexSource;
import org.hibernate.metamodel.spi.source.SizeSource;

/**
 *
 */
public class SequentialPluralAttributeIndexSourceImpl extends AbstractHbmSourceNode implements SequentialPluralAttributeIndexSource {
	private final List< RelationalValueSource > valueSources;
	private final HibernateTypeSource typeSource;
	private final int base;

	public SequentialPluralAttributeIndexSourceImpl(MappingDocument sourceMappingDocument, final JaxbListIndexElement indexElement) {
		super( sourceMappingDocument );
		valueSources = Helper.buildValueSources( sourceMappingDocument, new Helper.ValueSourcesAdapter() {

			List< JaxbColumnElement > columnElements = indexElement.getColumn() == null ? Collections.EMPTY_LIST : Collections.singletonList( indexElement.getColumn() );

			@Override
			public String getColumnAttribute() {
				return indexElement.getColumnAttribute();
			}

			@Override
			public List<JaxbColumnElement> getColumn() {
				return columnElements;
			}

			@Override
			public boolean isIncludedInInsertByDefault() {
				return areValuesIncludedInInsertByDefault();
			}

			@Override
			public boolean isIncludedInUpdateByDefault() {
				return areValuesIncludedInUpdateByDefault();
			}
		} );
		typeSource = new HibernateTypeSource() {

			@Override
			public String getName() {
				return "integer";
			}

			@Override
			public Map< String, String > getParameters() {
				return java.util.Collections.< String, String >emptyMap();
			}
			@Override
			public Class getJavaType() {
				return null;
			}
		};
		base = Integer.parseInt( indexElement.getBase() );
	}

	public SequentialPluralAttributeIndexSourceImpl(MappingDocument sourceMappingDocument, final JaxbIndexElement indexElement) {
		super( sourceMappingDocument );
		valueSources = Helper.buildValueSources( sourceMappingDocument, new Helper.ValueSourcesAdapter() {

			@Override
			public String getColumnAttribute() {
				return indexElement.getColumnAttribute();
			}

			@Override
			public SizeSource getSizeSource() {
				return Helper.createSizeSourceIfMapped(
						indexElement.getLength(),
						null,
						null
				);
			}
			@Override
			public List<JaxbColumnElement> getColumn() {
				return indexElement.getColumn();
			}

			@Override
			public boolean isIncludedInInsertByDefault() {
				return areValuesIncludedInInsertByDefault();
			}

			@Override
			public boolean isIncludedInUpdateByDefault() {
				return areValuesIncludedInUpdateByDefault();
			}
		} );
		typeSource = new HibernateTypeSource() {

			@Override
			public String getName() {
				return StringHelper.isEmpty( indexElement.getType() ) ? "integer" : indexElement.getType();
			}

			@Override
			public Map< String, String > getParameters() {
				return java.util.Collections.< String, String >emptyMap();
			}
			@Override
			public Class getJavaType() {
				return null;
			}
		};
		base = 0;
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return true;
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return false;
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return false;
	}
	@Override
	public int base() {
		return base;
	}

	@Override
	public PluralAttributeIndexBinding.Nature getNature() {
		return PluralAttributeIndexBinding.Nature.BASIC;
	}

	@Override
	public List<Binder.DefaultNamingStrategy> getDefaultNamingStrategies() {
		final Binder.DefaultNamingStrategy defaultNamingStrategy = 	new Binder.DefaultNamingStrategy() {
			@Override
			public String defaultName(NamingStrategy namingStrategy) {
				return namingStrategy.columnName( "idx" );
			}
		};
		return Collections.singletonList( defaultNamingStrategy );
	}

	@Override
	public HibernateTypeSource getTypeInformation() {
		return typeSource;
	}

	@Override
	public boolean isReferencedEntityAttribute() {
		return false;
	}

	@Override
	public List< RelationalValueSource > relationalValueSources() {
		return valueSources;
	}
}
