package org.hibernate.orm.test.multitenancy;

import java.util.HashMap;
import java.util.Map;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingException;

/**
 * An in-memory JNDI namespace, supplied to InitialContext through the test's
 * InitialContextFactory without installing a JVM-wide naming provider.
 */
class TestingJndiContext extends InitialContext {
	private final Map<String, Object> bindings = new HashMap<>();

	TestingJndiContext() throws NamingException {
		super( true );
	}

	@Override
	public Object lookup(Name name) throws NamingException {
		if ( name.isEmpty() ) {
			return this;
		}
		final var object = bindings.get( name.get( 0 ) );
		if ( object == null ) {
			throw new NameNotFoundException( name.toString() );
		}
		return name.size() == 1 ? object : ((Context) object).lookup( name.getSuffix( 1 ) );
	}

	@Override
	public Object lookup(String name) throws NamingException {
		return lookup( new CompositeName( name ) );
	}

	@Override
	public void bind(Name name, Object object) throws NamingException {
		final var parent = parent( name );
		final var key = name.get( name.size() - 1 );
		if ( parent.bindings.putIfAbsent( key, object ) != null ) {
			throw new NameAlreadyBoundException( name.toString() );
		}
	}

	@Override
	public void bind(String name, Object object) throws NamingException {
		bind( new CompositeName( name ), object );
	}

	@Override
	public void unbind(Name name) throws NamingException {
		parent( name ).bindings.remove( name.get( name.size() - 1 ) );
	}

	@Override
	public void unbind(String name) throws NamingException {
		unbind( new CompositeName( name ) );
	}

	@Override
	public Context createSubcontext(Name name) throws NamingException {
		final var context = new TestingJndiContext();
		bind( name, context );
		return context;
	}

	@Override
	public Context createSubcontext(String name) throws NamingException {
		return createSubcontext( new CompositeName( name ) );
	}

	@Override
	public NameParser getNameParser(String name) {
		return CompositeName::new;
	}

	@Override
	public void close() {
		// The namespace remains available to other InitialContext handles.
	}

	private TestingJndiContext parent(Name name) throws NamingException {
		return (TestingJndiContext) lookup( name.getPrefix( name.size() - 1 ) );
	}
}
