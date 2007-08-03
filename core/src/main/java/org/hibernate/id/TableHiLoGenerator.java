//$Id: TableHiLoGenerator.java 11123 2007-01-31 23:46:11Z steve.ebersole@jboss.com $
package org.hibernate.id;

import java.io.Serializable;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.HibernateException;
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

	private long hi;
	private int lo;
	private int maxLo;
	private Class returnClass;

	private static final Logger log = LoggerFactory.getLogger(TableHiLoGenerator.class);

	public void configure(Type type, Properties params, Dialect d) {
		super.configure(type, params, d);
		maxLo = PropertiesHelper.getInt(MAX_LO, params, Short.MAX_VALUE);
		lo = maxLo + 1; // so we "clock over" on the first invocation
		returnClass = type.getReturnedClass();
	}

	public synchronized Serializable generate(SessionImplementor session, Object obj) 
	throws HibernateException {
        if (maxLo < 1) {
			//keep the behavior consistent even for boundary usages
			long val = ( (Number) super.generate(session, obj) ).longValue();
			if (val == 0) val = ( (Number) super.generate(session, obj) ).longValue();
			return IdentifierGeneratorFactory.createNumber( val, returnClass );
		}
		if (lo>maxLo) {
			long hival = ( (Number) super.generate(session, obj) ).longValue();
			lo = (hival == 0) ? 1 : 0;
			hi = hival * (maxLo+1);
			log.debug("new hi value: " + hival);
		}

		return IdentifierGeneratorFactory.createNumber( hi + lo++, returnClass );

	}

}
