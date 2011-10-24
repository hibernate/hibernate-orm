/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.id;

import java.io.Serializable;
import java.util.Properties;
import java.util.UUID;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.UUIDTypeDescriptor;

/**
 * An {@link IdentifierGenerator} which generates {@link UUID} values using a pluggable
 * {@link UUIDGenerationStrategy generation strategy}.  The values this generator can return
 * include {@link UUID}, {@link String} and byte[16]
 * <p/>
 * Supports 2 config parameters:<ul>
 * <li>{@link #UUID_GEN_STRATEGY} - names the {@link UUIDGenerationStrategy} instance to use</li>
 * <li>{@link #UUID_GEN_STRATEGY_CLASS} - names the {@link UUIDGenerationStrategy} class to use</li>
 * </ul>
 * <p/>
 * Currently there are 2 standard implementations of {@link UUIDGenerationStrategy}:<ul>
 * <li>{@link StandardRandomStrategy} (the default, if none specified)</li>
 * <li>{@link org.hibernate.id.uuid.CustomVersionOneStrategy}</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class UUIDGenerator implements IdentifierGenerator, Configurable {
	public static final String UUID_GEN_STRATEGY = "uuid_gen_strategy";
	public static final String UUID_GEN_STRATEGY_CLASS = "uuid_gen_strategy_class";

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, UUIDGenerator.class.getName());

	private UUIDGenerationStrategy strategy;
	private UUIDTypeDescriptor.ValueTransformer valueTransformer;

	public static UUIDGenerator buildSessionFactoryUniqueIdentifierGenerator() {
		final UUIDGenerator generator = new UUIDGenerator();
		generator.strategy = StandardRandomStrategy.INSTANCE;
		generator.valueTransformer = UUIDTypeDescriptor.ToStringTransformer.INSTANCE;
		return generator;
	}

	public void configure(Type type, Properties params, Dialect d) throws MappingException {
		// check first for the strategy instance
		strategy = (UUIDGenerationStrategy) params.get( UUID_GEN_STRATEGY );
		if ( strategy == null ) {
			// next check for the strategy class
			final String strategyClassName = params.getProperty( UUID_GEN_STRATEGY_CLASS );
			if ( strategyClassName != null ) {
				try {
					final Class strategyClass = ReflectHelper.classForName( strategyClassName );
					try {
						strategy = (UUIDGenerationStrategy) strategyClass.newInstance();
					}
					catch ( Exception ignore ) {
                        LOG.unableToInstantiateUuidGenerationStrategy(ignore);
					}
				}
				catch ( ClassNotFoundException ignore ) {
                    LOG.unableToLocateUuidGenerationStrategy(strategyClassName);
				}
			}
		}
		if ( strategy == null ) {
			// lastly use the standard random generator
			strategy = StandardRandomStrategy.INSTANCE;
		}

		if ( UUID.class.isAssignableFrom( type.getReturnedClass() ) ) {
			valueTransformer = UUIDTypeDescriptor.PassThroughTransformer.INSTANCE;
		}
		else if ( String.class.isAssignableFrom( type.getReturnedClass() ) ) {
			valueTransformer = UUIDTypeDescriptor.ToStringTransformer.INSTANCE;
		}
		else if ( byte[].class.isAssignableFrom( type.getReturnedClass() ) ) {
			valueTransformer = UUIDTypeDescriptor.ToBytesTransformer.INSTANCE;
		}
		else {
			throw new HibernateException( "Unanticipated return type [" + type.getReturnedClass().getName() + "] for UUID conversion" );
		}
	}

	public Serializable generate(SessionImplementor session, Object object) throws HibernateException {
		return valueTransformer.transform( strategy.generateUUID( session ) );
	}
}
