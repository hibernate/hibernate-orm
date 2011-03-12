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
package org.hibernate.util;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.cfg.Environment;

public final class NamingHelper {

	private static final Logger log = LoggerFactory.getLogger(NamingHelper.class);

	public static InitialContext getInitialContext(Properties props) throws NamingException {

		Hashtable hash = getJndiProperties(props);
		log.info("JNDI InitialContext properties:" + hash);
		try {
			return hash.size()==0 ?
					new InitialContext() :
					new InitialContext(hash);
		}
		catch (NamingException e) {
			log.error("Could not obtain initial context", e);
			throw e;
		}
	}

	/**
	 * Bind val to name in ctx, and make sure that all intermediate contexts exist.
	 *
	 * @param ctx the root context
	 * @param name the name as a string
	 * @param val the object to be bound
	 * @throws NamingException
	 */
	public static void bind(Context ctx, String name, Object val) throws NamingException {
		try {
			log.trace("binding: " + name);
			ctx.rebind(name, val);
		}
		catch (Exception e) {
			Name n = ctx.getNameParser("").parse(name);
			while ( n.size() > 1 ) {
				String ctxName = n.get(0);

				Context subctx=null;
				try {
					log.trace("lookup: " + ctxName);
					subctx = (Context) ctx.lookup(ctxName);
				}
				catch (NameNotFoundException nfe) {}

				if (subctx!=null) {
					log.debug("Found subcontext: " + ctxName);
					ctx = subctx;
				}
				else {
					log.info("Creating subcontext: " + ctxName);
					ctx = ctx.createSubcontext(ctxName);
				}
				n = n.getSuffix(1);
			}
			log.trace("binding: " + n);
			ctx.rebind(n, val);
		}
		log.debug("Bound name: " + name);
	}

	/**
	 * Transform JNDI properties passed in the form <tt>hibernate.jndi.*</tt> to the
	 * format accepted by <tt>InitialContext</tt> by triming the leading "<tt>hibernate.jndi</tt>".
	 */
	public static Properties getJndiProperties(Properties properties) {

		HashSet specialProps = new HashSet();
		specialProps.add(Environment.JNDI_CLASS);
		specialProps.add(Environment.JNDI_URL);

		Iterator iter = properties.keySet().iterator();
		Properties result = new Properties();
		while ( iter.hasNext() ) {
			String prop = (String) iter.next();
			if ( prop.indexOf(Environment.JNDI_PREFIX) > -1 && !specialProps.contains(prop) ) {
				result.setProperty(
						prop.substring( Environment.JNDI_PREFIX.length()+1 ),
						properties.getProperty(prop)
					);
			}
		}

		String jndiClass = properties.getProperty(Environment.JNDI_CLASS);
		String jndiURL = properties.getProperty(Environment.JNDI_URL);
		// we want to be able to just use the defaults,
		// if JNDI environment properties are not supplied
		// so don't put null in anywhere
		if (jndiClass != null) result.put(Context.INITIAL_CONTEXT_FACTORY, jndiClass);
		if (jndiURL != null) result.put(Context.PROVIDER_URL, jndiURL);

		return result;
	}

	private NamingHelper() {}

}







