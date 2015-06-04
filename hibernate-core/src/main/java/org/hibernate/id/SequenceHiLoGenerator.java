/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.io.Serializable;
import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.enhanced.AccessCallback;
import org.hibernate.id.enhanced.LegacyHiLoAlgorithmOptimizer;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

/**
 * <b>seqhilo</b><br>
 * <br>
 * An <tt>IdentifierGenerator</tt> that combines a hi/lo algorithm with an underlying
 * oracle-style sequence that generates hi values. The user may specify a
 * maximum lo value to determine how often new hi values are fetched.<br>
 * <br>
 * Mapping parameters supported: sequence, max_lo, parameters.
 *
 * @author Gavin King
 *
 * @deprecated See deprecation discussion on {@link SequenceGenerator}
 */
@Deprecated
public class SequenceHiLoGenerator extends SequenceGenerator {
	public static final String MAX_LO = "max_lo";

	private int maxLo;

	private LegacyHiLoAlgorithmOptimizer hiloOptimizer;

	@Override
	public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) throws MappingException {
		super.configure( type, params, serviceRegistry );

		maxLo = ConfigurationHelper.getInt( MAX_LO, params, 9 );

		if ( maxLo >= 1 ) {
			hiloOptimizer = new LegacyHiLoAlgorithmOptimizer(
					getIdentifierType().getReturnedClass(),
					maxLo
			);
		}
	}

	@Override
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
					@Override
					public IntegralDataTypeHolder getNextValue() {
						return generateHolder( session );
					}

					@Override
					public String getTenantIdentifier() {
						return session.getTenantIdentifier();
					}
				}
		);
	}

	/**
	 * For testing/assertion purposes
	 *
	 * @return The optimizer
	 */
	LegacyHiLoAlgorithmOptimizer getHiloOptimizer() {
		return hiloOptimizer;
	}
}
