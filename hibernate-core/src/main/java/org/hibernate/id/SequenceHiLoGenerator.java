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

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.enhanced.AccessCallback;
import org.hibernate.id.enhanced.OptimizerFactory;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.type.Type;

/**
 * <b>seqhilo</b><br>
 * <br>
 * An <tt>IdentifierGenerator</tt> that combines a hi/lo algorithm with an underlying
 * oracle-style sequence that generates hi values. The user may specify a
 * maximum lo value to determine how often new hi values are fetched.<br>
 * <br>
 * If sequences are not available, <tt>TableHiLoGenerator</tt> might be an
 * alternative.<br>
 * <br>
 * Mapping parameters supported: sequence, max_lo, parameters.
 *
 * @see TableHiLoGenerator
 * @author Gavin King
 */
public class SequenceHiLoGenerator extends SequenceGenerator {
	public static final String MAX_LO = "max_lo";

	private int maxLo;

	private OptimizerFactory.LegacyHiLoAlgorithmOptimizer hiloOptimizer;

	public void configure(Type type, Properties params, Dialect d) throws MappingException {
		super.configure(type, params, d);

		maxLo = ConfigurationHelper.getInt( MAX_LO, params, 9 );

		if ( maxLo >= 1 ) {
			hiloOptimizer = new OptimizerFactory.LegacyHiLoAlgorithmOptimizer(
					getIdentifierType().getReturnedClass(),
					maxLo
			);
		}
	}

	public synchronized Serializable generate(final SessionImplementor session, Object obj) {
		// maxLo < 1 indicates a hilo generator with no hilo :?
		if ( maxLo < 1 ) {
			//keep the behavior consistent even for boundary usages
			IntegralDataTypeHolder value = null;
			while ( value == null || value.lt( 0 ) ) {
				value = super.generateHolder( session );
			}
			return value.makeValue();
		}

		return hiloOptimizer.generate(
				new AccessCallback() {
					public IntegralDataTypeHolder getNextValue() {
						return generateHolder( session );
					}
				}
		);
	}

	/**
	 * For testing/assertion purposes
	 *
	 * @return The optimizer
	 */
	OptimizerFactory.LegacyHiLoAlgorithmOptimizer getHiloOptimizer() {
		return hiloOptimizer;
	}
}
