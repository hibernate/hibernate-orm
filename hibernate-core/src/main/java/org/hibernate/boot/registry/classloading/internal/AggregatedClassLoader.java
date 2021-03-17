/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.registry.classloading.internal;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;

public class AggregatedClassLoader extends ClassLoader {
	private final ClassLoader[] individualClassLoaders;
	private final TcclLookupPrecedence tcclLookupPrecedence;

	public AggregatedClassLoader(final LinkedHashSet<ClassLoader> orderedClassLoaderSet, TcclLookupPrecedence precedence) {
		super( null );
		individualClassLoaders = orderedClassLoaderSet.toArray( new ClassLoader[orderedClassLoaderSet.size()] );
		tcclLookupPrecedence = precedence;
	}

	Iterator<ClassLoader> newClassLoaderIterator() {
		final ClassLoader threadClassLoader = locateTCCL();
		if ( tcclLookupPrecedence == TcclLookupPrecedence.NEVER || threadClassLoader == null ) {
			return newTcclNeverIterator();
		}
		else if ( tcclLookupPrecedence == TcclLookupPrecedence.AFTER ) {
			return newTcclAfterIterator(threadClassLoader);
		}
		else if ( tcclLookupPrecedence == TcclLookupPrecedence.BEFORE ) {
			return newTcclBeforeIterator(threadClassLoader);
		}
		else {
			throw new RuntimeException( "Unknown precedence: "+tcclLookupPrecedence );
		}
	}

	private Iterator<ClassLoader> newTcclBeforeIterator(final ClassLoader threadContextClassLoader) {
		final ClassLoader systemClassLoader = locateSystemClassLoader();
		return new Iterator<ClassLoader>() {
			private int currentIndex = 0;
			private boolean tcCLReturned = false;
			private boolean sysCLReturned = false;

			@Override
			public boolean hasNext() {
				if ( !tcCLReturned ) {
					return true;
				}
				else if ( currentIndex < individualClassLoaders.length ) {
					return true;
				}
				else if ( !sysCLReturned && systemClassLoader != null ) {
					return true;
				}

				return false;
			}

			@Override
			public ClassLoader next() {
				if ( !tcCLReturned ) {
					tcCLReturned = true;
					return threadContextClassLoader;
				}
				else if ( currentIndex < individualClassLoaders.length ) {
					currentIndex += 1;
					return individualClassLoaders[ currentIndex - 1 ];
				}
				else if ( !sysCLReturned && systemClassLoader != null ) {
					sysCLReturned = true;
					return systemClassLoader;
				}
				throw new IllegalStateException( "No more item" );
			}
		};
	}

	private Iterator<ClassLoader> newTcclAfterIterator(final ClassLoader threadContextClassLoader) {
		final ClassLoader systemClassLoader = locateSystemClassLoader();
		return new Iterator<ClassLoader>() {
			private int currentIndex = 0;
			private boolean tcCLReturned = false;
			private boolean sysCLReturned = false;

			@Override
			public boolean hasNext() {
				if ( currentIndex < individualClassLoaders.length ) {
					return true;
				}
				else if ( !tcCLReturned ) {
					return true;
				}
				else if ( !sysCLReturned && systemClassLoader != null ) {
					return true;
				}

				return false;
			}

			@Override
			public ClassLoader next() {
				if ( currentIndex < individualClassLoaders.length ) {
					currentIndex += 1;
					return individualClassLoaders[ currentIndex - 1 ];
				}
				else if ( !tcCLReturned ) {
					tcCLReturned = true;
					return threadContextClassLoader;
				}
				else if ( !sysCLReturned && systemClassLoader != null ) {
					sysCLReturned = true;
					return systemClassLoader;
				}
				throw new IllegalStateException( "No more item" );
			}
		};
	}

	private Iterator<ClassLoader> newTcclNeverIterator() {
		final ClassLoader systemClassLoader = locateSystemClassLoader();
		return new Iterator<ClassLoader>() {
			private int currentIndex = 0;
			private boolean sysCLReturned = false;

			@Override
			public boolean hasNext() {
				if ( currentIndex < individualClassLoaders.length ) {
					return true;
				}
				else if ( !sysCLReturned && systemClassLoader != null ) {
					return true;
				}

				return false;
			}

			@Override
			public ClassLoader next() {
				if ( currentIndex < individualClassLoaders.length ) {
					currentIndex += 1;
					return individualClassLoaders[ currentIndex - 1 ];
				}
				else if ( !sysCLReturned && systemClassLoader != null ) {
					sysCLReturned = true;
					return systemClassLoader;
				}
				throw new IllegalStateException( "No more item" );
			}
		};
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		final LinkedHashSet<URL> resourceUrls = new LinkedHashSet<URL>();
		final Iterator<ClassLoader> clIterator = newClassLoaderIterator();
		while ( clIterator.hasNext() ) {
			final ClassLoader classLoader = clIterator.next();
			final Enumeration<URL> urls = classLoader.getResources( name );
			while ( urls.hasMoreElements() ) {
				resourceUrls.add( urls.nextElement() );
			}
		}

		return new Enumeration<URL>() {
			final Iterator<URL> resourceUrlIterator = resourceUrls.iterator();

			@Override
			public boolean hasMoreElements() {
				return resourceUrlIterator.hasNext();
			}

			@Override
			public URL nextElement() {
				return resourceUrlIterator.next();
			}
		};
	}

	@Override
	protected URL findResource(String name) {
		final Iterator<ClassLoader> clIterator = newClassLoaderIterator();
		while ( clIterator.hasNext() ) {
			final ClassLoader classLoader = clIterator.next();
			final URL resource = classLoader.getResource( name );
			if ( resource != null ) {
				return resource;
			}
		}
		return super.findResource( name );
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		final Iterator<ClassLoader> clIterator = newClassLoaderIterator();
		while ( clIterator.hasNext() ) {
			final ClassLoader classLoader = clIterator.next();
			try {
				return classLoader.loadClass( name );
			}
			catch (Exception ignore) {
			}
			catch (LinkageError ignore) {
			}
		}

		throw new ClassNotFoundException( "Could not load requested class : " + name );
	}

	private static ClassLoader locateSystemClassLoader() {
		try {
			return ClassLoader.getSystemClassLoader();
		}
		catch (Exception e) {
			return null;
		}
	}

	private static ClassLoader locateTCCL() {
		try {
			return Thread.currentThread().getContextClassLoader();
		}
		catch (Exception e) {
			return null;
		}
	}

}
