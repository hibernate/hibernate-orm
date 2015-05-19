/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.jndi;

import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.hibernate.engine.jndi.internal.JndiServiceImpl;

/**
 * Helper for dealing with JNDI.
 *
 * @deprecated As JNDI access should get routed through {@link org.hibernate.engine.jndi.spi.JndiService}
 */
@Deprecated
public final class JndiHelper {
	private JndiHelper() {
	}

	/**
	 * Given a hodgepodge of properties, extract out the ones relevant for JNDI interaction.
	 *
	 * @param configurationValues The map of config values
	 *
	 * @return The extracted JNDI specific properties.
	 */
	@SuppressWarnings({ "unchecked" })
	public static Properties extractJndiProperties(Map configurationValues) {
		return JndiServiceImpl.extractJndiProperties( configurationValues );
	}

	public static InitialContext getInitialContext(Properties props) throws NamingException {
		final Hashtable hash = extractJndiProperties( props );
		return hash.size() == 0 ? new InitialContext() : new InitialContext( hash );
	}

	/**
	 * Bind val to name in ctx, and make sure that all intermediate contexts exist.
	 *
	 * @param ctx the root context
	 * @param name the name as a string
	 * @param val the object to be bound
	 *
	 * @throws NamingException Indicates a problem performing the bind.
	 */
	public static void bind(Context ctx, String name, Object val) throws NamingException {
		try {
			ctx.rebind(name, val);
		}
		catch (Exception e) {
			Name n = ctx.getNameParser( "" ).parse( name );
			while ( n.size() > 1 ) {
				final String ctxName = n.get( 0 );

				Context subctx = null;
				try {
					subctx = (Context) ctx.lookup( ctxName );
				}
				catch (NameNotFoundException ignore) {
				}

				if ( subctx != null ) {
					ctx = subctx;
				}
				else {
					ctx = ctx.createSubcontext( ctxName );
				}
				n = n.getSuffix( 1 );
			}
			ctx.rebind( n, val );
		}
	}
}

