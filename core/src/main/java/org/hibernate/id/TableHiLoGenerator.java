/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.id;

import java.io.Serializable;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.Type;
import org.hibernate.util.PropertiesHelper;

/**
 * <b>hilo</b><br>
 * <br>
 * An <tt>IdentifierGenerator</tt> that returns a <tt>Long</tt>, constructed using
 * a hi/lo algorithm. The hi value MUST be fetched in a seperate transaction
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

	private int maxLo;
	private int lo;

	private IntegralDataTypeHolder value;

	private static final Logger log = LoggerFactory.getLogger(TableHiLoGenerator.class);

	public void configure(Type type, Properties params, Dialect d) {
		super.configure(type, params, d);
		maxLo = PropertiesHelper.getInt(MAX_LO, params, Short.MAX_VALUE);
		lo = maxLo + 1; // so we "clock over" on the first invocation
	}

	public synchronized Serializable generate(SessionImplementor session, Object obj) {
		// maxLo < 1 indicates a hilo generator with no hilo :?
        if ( maxLo < 1 ) {
			//keep the behavior consistent even for boundary usages
			IntegralDataTypeHolder value = null;
			while ( value == null || value.lt( 0 ) ) {
				value = generateHolder( session );
			}
			return value.makeValue();
		}

		if ( lo > maxLo ) {
			IntegralDataTypeHolder hiVal = generateHolder( session );
			lo = ( hiVal.eq( 0 ) ) ? 1 : 0;
			value = hiVal.copy().multiplyBy( maxLo+1 ).add( lo );
			if ( log.isDebugEnabled() ) {
				log.debug("new hi value: " + hiVal);
			}
		}

		return value.makeValueThenIncrement();
	}

}
