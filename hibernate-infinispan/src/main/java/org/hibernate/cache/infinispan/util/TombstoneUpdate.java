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

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class TombstoneUpdate<T> {
	public static TombstoneUpdate EVICT = new TombstoneUpdate(Long.MIN_VALUE, null);
	private long timestamp;
	private T value;

	public TombstoneUpdate(long timestamp, T value) {
		this.timestamp = timestamp;
		this.value = value;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public T getValue() {
		return value;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("TombstoneUpdate{");
		sb.append("timestamp=").append(timestamp);
		sb.append(", value=").append(value);
		sb.append('}');
		return sb.toString();
	}

	public static class Externalizer implements AdvancedExternalizer<TombstoneUpdate> {
		@Override
		public Set<Class<? extends TombstoneUpdate>> getTypeClasses() {
			return Collections.<Class<? extends TombstoneUpdate>>singleton(TombstoneUpdate.class);
		}

		@Override
		public Integer getId() {
			return Externalizers.TOMBSTONE_UPDATE;
		}

		@Override
		public void writeObject(ObjectOutput output, TombstoneUpdate object) throws IOException {
			output.writeObject(object.getValue());
			if (object.getValue() != null) {
				output.writeLong(object.getTimestamp());
			}
		}

		@Override
		public TombstoneUpdate readObject(ObjectInput input) throws IOException, ClassNotFoundException {
			Object value = input.readObject();
			if (value != null) {
				long timestamp = input.readLong();
				return new TombstoneUpdate(timestamp, value);
			}
			else {
				return EVICT;
			}
		}
	}
}
