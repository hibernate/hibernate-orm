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
package org.hibernate.metamodel.source.hbm;

import org.hibernate.EntityMode;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbCacheElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbHibernateMapping;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.binding.Caching;
import org.hibernate.metamodel.binding.IdGenerator;
import org.hibernate.metamodel.source.MappingException;
import org.hibernate.metamodel.source.binder.DiscriminatorSource;
import org.hibernate.metamodel.source.binder.IdentifierSource;
import org.hibernate.metamodel.source.binder.RelationalValueSource;
import org.hibernate.metamodel.source.binder.RootEntitySource;
import org.hibernate.metamodel.source.binder.SimpleIdentifierSource;
import org.hibernate.metamodel.source.binder.SingularAttributeSource;
import org.hibernate.metamodel.source.binder.TableSource;

/**
 * @author Steve Ebersole
 */
public class RootEntitySourceImpl extends AbstractEntitySourceImpl implements RootEntitySource {
	protected RootEntitySourceImpl(MappingDocument sourceMappingDocument, JaxbHibernateMapping.JaxbClass entityElement) {
		super( sourceMappingDocument, entityElement );
	}

	@Override
	protected JaxbHibernateMapping.JaxbClass entityElement() {
		return (JaxbHibernateMapping.JaxbClass) super.entityElement();
	}

	@Override
	public IdentifierSource getIdentifierSource() {
		if ( entityElement().getId() != null ) {
			return new SimpleIdentifierSource() {
				@Override
				public SingularAttributeSource getIdentifierAttributeSource() {
					return new SingularIdentifierAttributeSourceImpl(
							entityElement().getId(),
							sourceMappingDocument().getMappingLocalBindingContext()
					);
				}

				@Override
				public IdGenerator getIdentifierGeneratorDescriptor() {
					if ( entityElement().getId().getGenerator() != null ) {
						final String generatorName = entityElement().getId().getGenerator().getClazz();
						IdGenerator idGenerator = sourceMappingDocument().getMappingLocalBindingContext()
								.getMetadataImplementor()
								.getIdGenerator( generatorName );
						if ( idGenerator == null ) {
							idGenerator = new IdGenerator(
									getEntityName() + generatorName,
									generatorName,
									Helper.extractParameters( entityElement().getId().getGenerator().getParam() )
							);
						}
						return idGenerator;
					}
					return null;
				}

				@Override
				public Nature getNature() {
					return Nature.SIMPLE;
				}
			};
		}
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public SingularAttributeSource getVersioningAttributeSource() {
		if ( entityElement().getVersion() != null ) {
			return new VersionAttributeSourceImpl(
					entityElement().getVersion(),
					sourceMappingDocument().getMappingLocalBindingContext()
			);
		}
		else if ( entityElement().getTimestamp() != null ) {
			return new TimestampAttributeSourceImpl(
					entityElement().getTimestamp(),
					sourceMappingDocument().getMappingLocalBindingContext()
			);
		}
		return null;
	}

	@Override
	public EntityMode getEntityMode() {
		return determineEntityMode();
	}

	@Override
	public boolean isMutable() {
		return entityElement().isMutable();
	}


	@Override
	public boolean isExplicitPolymorphism() {
		return "explicit".equals( entityElement().getPolymorphism() );
	}

	@Override
	public String getWhere() {
		return entityElement().getWhere();
	}

	@Override
	public String getRowId() {
		return entityElement().getRowid();
	}

	@Override
	public OptimisticLockStyle getOptimisticLockStyle() {
		final String optimisticLockModeString = Helper.getStringValue( entityElement().getOptimisticLock(), "version" );
		try {
			return OptimisticLockStyle.valueOf( optimisticLockModeString.toUpperCase() );
		}
		catch ( Exception e ) {
			throw new MappingException(
					"Unknown optimistic-lock value : " + optimisticLockModeString,
					sourceMappingDocument().getOrigin()
			);
		}
	}

	@Override
	public Caching getCaching() {
		final JaxbCacheElement cache = entityElement().getCache();
		if ( cache == null ) {
			return null;
		}
		final String region = cache.getRegion() != null ? cache.getRegion() : getEntityName();
		final AccessType accessType = Enum.valueOf( AccessType.class, cache.getUsage() );
		final boolean cacheLazyProps = !"non-lazy".equals( cache.getInclude() );
		return new Caching( region, accessType, cacheLazyProps );
	}

	@Override
	public TableSource getPrimaryTable() {
		return new TableSource() {
			@Override
			public String getExplicitSchemaName() {
				return entityElement().getSchema();
			}

			@Override
			public String getExplicitCatalogName() {
				return entityElement().getCatalog();
			}

			@Override
			public String getExplicitTableName() {
				return entityElement().getTable();
			}

			@Override
			public String getLogicalName() {
				// logical name for the primary table is null
				return null;
			}
		};
	}

	@Override
	public String getDiscriminatorMatchValue() {
		return entityElement().getDiscriminatorValue();
	}

	@Override
	public DiscriminatorSource getDiscriminatorSource() {
		final JaxbHibernateMapping.JaxbClass.JaxbDiscriminator discriminatorElement = entityElement().getDiscriminator();
		if ( discriminatorElement == null ) {
			return null;
		}

		return new DiscriminatorSource() {
			@Override
			public RelationalValueSource getDiscriminatorRelationalValueSource() {
				if ( StringHelper.isNotEmpty( discriminatorElement.getColumnAttribute() ) ) {
					return new ColumnAttributeSourceImpl(
							null, // root table
							discriminatorElement.getColumnAttribute(),
							discriminatorElement.isInsert(),
							discriminatorElement.isInsert()
					);
				}
				else if ( StringHelper.isNotEmpty( discriminatorElement.getFormulaAttribute() ) ) {
					return new FormulaImpl( null, discriminatorElement.getFormulaAttribute() );
				}
				else if ( discriminatorElement.getColumn() != null ) {
					return new ColumnSourceImpl(
							null, // root table
							discriminatorElement.getColumn(),
							discriminatorElement.isInsert(),
							discriminatorElement.isInsert()
					);
				}
				else if ( StringHelper.isNotEmpty( discriminatorElement.getFormula() ) ) {
					return new FormulaImpl( null, discriminatorElement.getFormula() );
				}
				else {
					throw new MappingException( "could not determine source of discriminator mapping", getOrigin() );
				}
			}

			@Override
			public String getExplicitHibernateTypeName() {
				return discriminatorElement.getType();
			}

			@Override
			public boolean isForced() {
				return discriminatorElement.isForce();
			}

			@Override
			public boolean isInserted() {
				return discriminatorElement.isInsert();
			}
		};
	}
}
