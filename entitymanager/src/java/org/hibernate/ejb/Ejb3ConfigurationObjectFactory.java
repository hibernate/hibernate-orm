//$Id: $
package org.hibernate.ejb;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

/**
 * @author Emmanuel Bernard
 */
public class Ejb3ConfigurationObjectFactory implements ObjectFactory {
	public Object getObjectInstance(
			Object reference, Name name, Context nameCtx, Hashtable<?, ?> environment
	) throws Exception {
		byte[] serialized = (byte[]) ( (Reference) reference ).get(0).getContent();
		ByteArrayInputStream byteIn = new ByteArrayInputStream( serialized );
		ObjectInputStream in = new ObjectInputStream( byteIn );
		Ejb3Configuration cfg = (Ejb3Configuration) in.readObject();
		in.close();
		byteIn.close();
		return cfg;
	}
}
