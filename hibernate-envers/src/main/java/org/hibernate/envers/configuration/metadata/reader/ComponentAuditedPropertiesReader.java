package org.hibernate.envers.configuration.metadata.reader;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.envers.Audited;
import org.hibernate.envers.ModificationStore;
import org.hibernate.envers.configuration.GlobalConfiguration;

/**
 * Reads the audited properties for components.
 * 
 * @author Hern&aacut;n Chanfreau
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class ComponentAuditedPropertiesReader extends AuditedPropertiesReader {

	public ComponentAuditedPropertiesReader(ModificationStore defaultStore,
			PersistentPropertiesSource persistentPropertiesSource,
			AuditedPropertiesHolder auditedPropertiesHolder,
			GlobalConfiguration globalCfg, ReflectionManager reflectionManager,
			String propertyNamePrefix) {
		super(defaultStore, persistentPropertiesSource, auditedPropertiesHolder,
				globalCfg, reflectionManager, propertyNamePrefix);
	}
	
	@Override
	protected boolean checkAudited(XProperty property,
			PropertyAuditingData propertyData, Audited allClassAudited) {
		// Checking if this property is explicitly audited or if all properties are.
		Audited aud = property.getAnnotation(Audited.class);
		if (aud != null) {
			propertyData.setStore(aud.modStore());
			propertyData.setRelationTargetAuditMode(aud.targetAuditMode());
			propertyData.setUsingModifiedFlag(checkUsingModifiedFlag(aud));
		} else {
			propertyData.setStore(ModificationStore.FULL);
		}	
	   return true;	
	}

}
