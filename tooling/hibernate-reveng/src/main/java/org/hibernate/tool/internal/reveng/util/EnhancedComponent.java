package org.hibernate.tool.internal.reveng.util;

import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;

@SuppressWarnings("serial")
public class EnhancedComponent extends Component implements EnhancedValue {

	private Properties idGenProps = new Properties();
	private String genStrategy = null;

	public EnhancedComponent(MetadataBuildingContext metadata, PersistentClass owner) throws MappingException {
		super(metadata, owner);
	}

	@Override
	public void setIdentifierGeneratorProperties(Properties props) {
		idGenProps = props;
		
	}
	
	@Override
	public Properties getIdentifierGeneratorProperties() {
		return idGenProps;
	}

	@Override
	public void setIdentifierGeneratorStrategy(String s) {
		genStrategy = s;
	}

	@Override
	public String getIdentifierGeneratorStrategy() {
		return genStrategy;
	}

	@Override
	public Class<?> getComponentClass() throws MappingException {
		// we prevent ORM from trying to load a component class by name,
		// since at the point when we are building these, a corresponding class is not yet created
		// (so can't even think about it being compiled and able to load via any classloader) ...
		return Object.class;
	}
}
