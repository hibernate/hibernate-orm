/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.util;

import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.commons.util.Util;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class Externalizers {

	public final static int UUID = 1200;

	public final static AdvancedExternalizer[] ALL_EXTERNALIZERS = new AdvancedExternalizer[] {
			new UUIDExternalizer()
	};

	public static class UUIDExternalizer implements AdvancedExternalizer<UUID> {

		@Override
		public Set<Class<? extends UUID>> getTypeClasses() {
			return Collections.<Class<? extends UUID>>singleton(UUID.class);
		}

		@Override
		public Integer getId() {
			return UUID;
		}

		@Override
		public void writeObject(ObjectOutput output, UUID uuid) throws IOException {
			output.writeLong(uuid.getMostSignificantBits());
			output.writeLong(uuid.getLeastSignificantBits());
		}

		@Override
		public UUID readObject(ObjectInput input) throws IOException, ClassNotFoundException {
			return new UUID(input.readLong(), input.readLong());
		}
	}
}
