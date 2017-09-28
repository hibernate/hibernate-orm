// $Id$
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.persistence.spi.PersistenceUnitTransactionType;

/**
 * Simple represenation of persistence.xml
 *
 * Object used by JBoss EJB 3 for persistence.xml parsing
 * Object used by Hibernate OGM as well, consider this some kind of exposed service at the SPI level
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 */
public class PersistenceMetadata {

	private String name;
	private String nonJtaDatasource;
	private String jtaDatasource;
	private String provider;
	private PersistenceUnitTransactionType transactionType;
	private boolean useQuotedIdentifiers = false; // the spec (erroneously?) calls this delimited-identifiers
	private List<String> classes = new ArrayList<String>();
	private List<String> packages = new ArrayList<String>();
	private List<String> mappingFiles = new ArrayList<String>();
	private Set<String> jarFiles = new HashSet<String>();
	private List<NamedInputStream> hbmfiles = new ArrayList<NamedInputStream>();
	private Properties props = new Properties();
	private boolean excludeUnlistedClasses = false;
	private String validationMode;
	private String version;

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getSharedCacheMode() {
		return sharedCacheMode;
	}

	public boolean isExcludeUnlistedClasses() {
		return excludeUnlistedClasses;
	}

	private String sharedCacheMode;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public PersistenceUnitTransactionType getTransactionType() {
		return transactionType;
	}

	public void setTransactionType(PersistenceUnitTransactionType transactionType) {
		this.transactionType = transactionType;
	}

	public String getNonJtaDatasource() {
		return nonJtaDatasource;
	}

	public void setNonJtaDatasource(String nonJtaDatasource) {
		this.nonJtaDatasource = nonJtaDatasource;
	}

	public String getJtaDatasource() {
		return jtaDatasource;
	}

	public void setJtaDatasource(String jtaDatasource) {
		this.jtaDatasource = jtaDatasource;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		if ( provider != null && provider.endsWith( ".class" ) ) {
			this.provider = provider.substring( 0, provider.length() - 6 );
		}
		this.provider = provider;
	}

	public boolean isUseQuotedIdentifiers() {
		return useQuotedIdentifiers;
	}

	public void setUseQuotedIdentifiers(boolean useQuotedIdentifiers) {
		this.useQuotedIdentifiers = useQuotedIdentifiers;
	}

	public List<String> getClasses() {
		return classes;
	}

	public void setClasses(List<String> classes) {
		this.classes = classes;
	}

	public List<String> getPackages() {
		return packages;
	}

	public void setPackages(List<String> packages) {
		this.packages = packages;
	}

	public List<String> getMappingFiles() {
		return mappingFiles;
	}

	public void setMappingFiles(List<String> mappingFiles) {
		this.mappingFiles = mappingFiles;
	}

	public Set<String> getJarFiles() {
		return jarFiles;
	}

	public void setJarFiles(Set<String> jarFiles) {
		this.jarFiles = jarFiles;
	}

	public Properties getProps() {
		return props;
	}

	public void setProps(Properties props) {
		this.props = props;
	}

	public List<NamedInputStream> getHbmfiles() {
		return hbmfiles;
	}

	/**
	 * @deprecated use getHbmfiles() rather
	 */
	public void setHbmfiles(List<NamedInputStream> hbmfiles) {
		this.hbmfiles = hbmfiles;
	}

	public boolean getExcludeUnlistedClasses() {
		return excludeUnlistedClasses;
	}

	public void setExcludeUnlistedClasses(boolean excludeUnlistedClasses) {
		this.excludeUnlistedClasses = excludeUnlistedClasses;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( "PersistenceMetadata(version=" )
				.append( version )
				.append(") [\n")
				.append("\tname: ").append(name).append("\n")
				.append("\tjtaDataSource: ").append(jtaDatasource).append("\n")
				.append("\tnonJtaDataSource: ").append(nonJtaDatasource).append("\n")
				.append("\ttransactionType: ").append(transactionType).append("\n")
				.append("\tprovider: ").append(provider).append("\n")
				.append("\tuseQuotedIdentifiers: ").append(useQuotedIdentifiers).append("\n")
				.append("\tclasses[\n");
		if (classes != null) {
			for (String elt : classes) {
				sb.append("\t\t").append( elt );
			}
		}
		sb.append( "\t]\n")
				.append("\tpackages[\n");
		if (packages != null) {
			for (String elt : packages) {
				sb.append("\t\t").append( elt ).append("\n");
			}
		}
		sb.append( "\t]\n")
				.append("\tmappingFiles[\n");
		if (mappingFiles != null) {
			for (String elt : mappingFiles) {
				sb.append("\t\t").append( elt ).append("\n");
			}
		}
		sb.append( "\t]\n")
				.append("\tjarFiles[\n");
		if (jarFiles != null) {
			for (String elt : jarFiles) {
				sb.append("\t\t").append( elt ).append("\n");
			}
		}
		sb.append( "\t]\n")
				.append("\thbmfiles: ")
				.append( hbmfiles != null ? hbmfiles.size() : 0 ).append("\n");

		if (validationMode != null) {
			sb.append("\tvalidation-mode: ").append(validationMode).append("\n");
		}
		if (sharedCacheMode != null) {
			sb.append("\tshared-cache-mode: ").append(sharedCacheMode).append("\n");
		}

		sb.append("\tproperties[\n");

		if (props != null) {
			for ( Map.Entry elt : props.entrySet()) {
				sb.append("\t\t").append( elt.getKey() ).append(": ").append( elt.getValue() ).append("\n");
			}
		}
		sb.append( "\t]").append( "]");

		return sb.toString();
	}

	public void setValidationMode(String validationMode) {
		this.validationMode = validationMode;
	}

	public String getValidationMode() {
		return validationMode;
	}

	public void setSharedCacheMode(String sharedCacheMode) {
		this.sharedCacheMode = sharedCacheMode;
	}
}
