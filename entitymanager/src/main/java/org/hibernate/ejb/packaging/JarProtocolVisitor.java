//$
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
