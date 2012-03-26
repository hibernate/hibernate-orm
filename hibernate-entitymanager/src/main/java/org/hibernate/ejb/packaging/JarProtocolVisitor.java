/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
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
 */
package org.hibernate.ejb.packaging;

import java.io.IOException;
import java.net.URL;
import java.util.Set;

import org.hibernate.annotations.common.AssertionFailure;

/**
 * @author Emmanuel Bernard
 */
public class JarProtocolVisitor implements JarVisitor {
	private JarVisitor delegate;
	private URL jarUrl;
	private Filter[] filters;

	public JarProtocolVisitor(URL url, Filter[] filters, String entry) {
		this.jarUrl = url;
		this.filters = filters;
		if (entry != null && entry.length() > 0) throw new IllegalArgumentException( "jar:jar: not supported: " + jarUrl );
		init();
	}

	private void init() {
		String file = jarUrl.getFile();
		String entry;
		int subEntryIndex = file.lastIndexOf( "!" );
		if (subEntryIndex == -1) throw new AssertionFailure("JAR URL does not contain '!/' :" + jarUrl);
		if ( subEntryIndex + 1 >= file.length() ) {
			entry = "";
		}
		else {
			entry = file.substring( subEntryIndex + 1 );
		}
		URL fileUrl = JarVisitorFactory.getJarURLFromURLEntry( jarUrl, entry );
		delegate = JarVisitorFactory.getVisitor( fileUrl, filters, entry );
		
	}

	public String getUnqualifiedJarName() {
		return delegate.getUnqualifiedJarName();
	}

	public Filter[] getFilters() {
		return delegate.getFilters();
	}

	public Set[] getMatchingEntries() throws IOException {
		return delegate.getMatchingEntries();
	}

}
