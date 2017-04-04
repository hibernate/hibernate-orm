/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.util;

import org.infinispan.commons.marshall.AdvancedExternalizer;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * Request to update the tombstone, coming from insert/update/remove operation.
 *
 * This object should *not* be stored in cache.
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class FutureUpdate {
	private final UUID uuid;
	private final long timestamp;
	private final Object value;

	public FutureUpdate(UUID uuid, long timestamp, Object value) {
		this.uuid = uuid;
		this.timestamp = timestamp;
		this.value = value;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("FutureUpdate{");
		sb.append("uuid=").append(uuid);
		sb.append(", timestamp=").append(timestamp);
		sb.append(", value=").append(value);
		sb.append('}');
		return sb.toString();
	}

	public UUID getUuid() {
		return uuid;
	}

	public Object getValue() {
		return value;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public static class Externalizer implements AdvancedExternalizer<FutureUpdate> {

		@Override
		public void writeObject(ObjectOutput output, FutureUpdate object) throws IOException {
			output.writeLong(object.uuid.getMostSignificantBits());
			output.writeLong(object.uuid.getLeastSignificantBits());
			output.writeLong(object.timestamp);
			output.writeObject(object.value);
		}

		@Override
		public FutureUpdate readObject(ObjectInput input) throws IOException, ClassNotFoundException {
			long msb = input.readLong();
			long lsb = input.readLong();
			long timestamp = input.readLong();
			Object value = input.readObject();
			return new FutureUpdate(new UUID(msb, lsb), timestamp, value);
		}

		@Override
		public Set<Class<? extends FutureUpdate>> getTypeClasses() {
			return Collections.<Class<? extends FutureUpdate>>singleton(FutureUpdate.class);
		}

		@Override
		public Integer getId() {
			return Externalizers.FUTURE_UPDATE;
		}
	}
}
