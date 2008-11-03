package org.hibernate.ejb.packaging;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Map;
import javax.persistence.spi.PersistenceUnitTransactionType;

/**
 * Simple represenation of persistence.xml
 *
 * Object used by JBoss EJB 3 for persistence.xml parsing
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision: 11329 $
 */
public class PersistenceMetadata {

	private String name;
	private String nonJtaDatasource;
	private String jtaDatasource;
	private String provider;
	private PersistenceUnitTransactionType transactionType;
	private List<String> classes = new ArrayList<String>();
	private List<String> packages = new ArrayList<String>();
	private List<String> mappingFiles = new ArrayList<String>();
	private Set<String> jarFiles = new HashSet<String>();
	private List<NamedInputStream> hbmfiles = new ArrayList<NamedInputStream>();
	private Properties props = new Properties();
	private boolean excludeUnlistedClasses = false;

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
		sb.append( "PersistenceMetadata [\n")
				.append("\tname: ").append(name).append("\n")
				.append("\tjtaDataSource: ").append(jtaDatasource).append("\n")
				.append("\tnonJtaDataSource: ").append(nonJtaDatasource).append("\n")
				.append("\ttransactionType: ").append(transactionType).append("\n")
				.append("\tprovider: ").append(provider).append("\n")
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
				.append( hbmfiles != null ? hbmfiles.size() : 0 ).append("\n")
				.append("\tproperties[\n");

		if (props != null) {
			for ( Map.Entry elt : props.entrySet()) {
				sb.append("\t\t").append( elt.getKey() ).append(": ").append( elt.getValue() ).append("\n");
			}
		}
		sb.append( "\t]").append( "]");

		return sb.toString();
	}
}
