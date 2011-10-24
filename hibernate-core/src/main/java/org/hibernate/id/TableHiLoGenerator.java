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

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.enhanced.AccessCallback;
import org.hibernate.id.enhanced.OptimizerFactory;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.type.Type;

/**
 * <b>hilo</b><br>
 * <br>
 * An <tt>IdentifierGenerator</tt> that returns a <tt>Long</tt>, constructed using
 * a hi/lo algorithm. The hi value MUST be fetched in a separate transaction
 * to the <tt>Session</tt> transaction so the generator must be able to obtain
 * a new connection and commit it. Hence this implementation may not
 * be used  when the user is supplying connections. In this
 * case a <tt>SequenceHiLoGenerator</tt> would be a better choice (where
 * supported).<br>
 * <br>
 * Mapping parameters supported: table, column, max_lo
 *
 * @see SequenceHiLoGenerator
 * @author Gavin King
 */
public class TableHiLoGenerator extends TableGenerator {
	/**
	 * The max_lo parameter
	 */
	public static final String MAX_LO = "max_lo";

	private OptimizerFactory.LegacyHiLoAlgorithmOptimizer hiloOptimizer;

	private int maxLo;

	public void configure(Type type, Properties params, Dialect d) {
		super.configure(type, params, d);
		maxLo = ConfigurationHelper.getInt(MAX_LO, params, Short.MAX_VALUE);

		if ( maxLo >= 1 ) {
			hiloOptimizer = new OptimizerFactory.LegacyHiLoAlgorithmOptimizer( type.getReturnedClass(), maxLo );
		}
	}

	public synchronized Serializable generate(final SessionImplementor session, Object obj) {
		// maxLo < 1 indicates a hilo generator with no hilo :?
        if ( maxLo < 1 ) {
			//keep the behavior consistent even for boundary usages
			IntegralDataTypeHolder value = null;
			while ( value == null || value.lt( 0 ) ) {
				value = generateHolder( session );
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

}
