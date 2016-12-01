/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.util;

import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.filter.KeyValueFilter;
import org.infinispan.metadata.Metadata;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * This is used both as the storage in entry, and for efficiency also directly in the cache.put() commands.
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class Tombstone {
	public static final ExcludeTombstonesFilter EXCLUDE_TOMBSTONES = new ExcludeTombstonesFilter();

	// the format of data is repeated (timestamp, UUID.LSB, UUID.MSB)
	private final long[] data;

	public Tombstone(UUID uuid, long timestamp) {
		this.data = new long[] { timestamp, uuid.getLeastSignificantBits(), uuid.getMostSignificantBits() };
	}

	private Tombstone(long[] data) {
		this.data = data;
	}

	public long getLastTimestamp() {
		long max = data[0];
		for (int i = 3; i < data.length; i += 3) {
			max = Math.max(max, data[i]);
		}
		return max;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Tombstone{");
		for (int i = 0; i < data.length; i += 3) {
			if (i != 0) {
				sb.append(", ");
			}
			sb.append(new UUID(data[i + 2], data[i + 1])).append('=').append(data[i]);
		}
		sb.append('}');
		return sb.toString();
	}

	public Tombstone merge(Tombstone update) {
		assert update != null;
		assert update.data.length == 3;
		int toRemove = 0;
		for (int i = 0; i < data.length; i += 3) {
			if (data[i] < update.data[0]) {
				toRemove += 3;
			}
			else if (update.data[1] == data[i + 1] && update.data[2] == data[i + 2]) {
				// UUID matches - second update during retry?
				toRemove += 3;
			}
		}
		if (data.length == toRemove) {
			// applying the update second time?
			return update;
		}
		else {
			long[] newData = new long[data.length - toRemove + 3]; // 3 for the update
			int j = 0;
			boolean uuidMatch = false;
			for (int i = 0; i < data.length; i += 3) {
				if (data[i] < update.data[0]) {
					// This is an old eviction
					continue;
				}
				else if (update.data[1] == data[i + 1] && update.data[2] == data[i + 2]) {
					// UUID matches
					System.arraycopy(update.data, 0, newData, j, 3);
					uuidMatch = true;
					j += 3;
				}
				else {
					System.arraycopy(data, i, newData, j, 3);
					j += 3;
				}
			}
			assert (uuidMatch && j == newData.length) || (!uuidMatch && j == newData.length - 3);
			if (!uuidMatch) {
				System.arraycopy(update.data, 0, newData, j, 3);
			}
			return new Tombstone(newData);
		}
	}

	public Object applyUpdate(UUID uuid, long timestamp, Object value) {
		int toRemove = 0;
		for (int i = 0; i < data.length; i += 3) {
			if (data[i] < timestamp) {
				toRemove += 3;
			}
			else if (uuid.getLeastSignificantBits() == data[i + 1] && uuid.getMostSignificantBits() == data[i + 2]) {
				toRemove += 3;
			}
		}
		if (data.length == toRemove) {
			if (value == null) {
				return new Tombstone(uuid, timestamp);
			}
			else {
				return value;
			}
		}
		else {
			long[] newData = new long[data.length - toRemove + 3]; // 3 for the update
			int j = 0;
			boolean uuidMatch = false;
			for (int i = 0; i < data.length; i += 3) {
				if (data[i] < timestamp) {
					// This is an old eviction
					continue;
				}
				else if (uuid.getLeastSignificantBits() == data[i + 1] && uuid.getMostSignificantBits() == data[i + 2]) {
					newData[j] = timestamp;
					newData[j + 1] = uuid.getLeastSignificantBits();
					newData[j + 2] = uuid.getMostSignificantBits();
					uuidMatch = true;
					j += 3;
				}
				else {
					System.arraycopy(data, i, newData, j, 3);
					j += 3;
				}
			}
			assert (uuidMatch && j == newData.length) || (!uuidMatch && j == newData.length - 3);
			if (!uuidMatch) {
				newData[j] = timestamp;
				newData[j + 1] = uuid.getLeastSignificantBits();
				newData[j + 2] = uuid.getMostSignificantBits();
			}
			return new Tombstone(newData);
		}
	}

	// Used only for testing purposes
	public int size() {
		return data.length / 3;
	}

	public static class Externalizer implements AdvancedExternalizer<Tombstone> {
		@Override
		public Set<Class<? extends Tombstone>> getTypeClasses() {
			return Collections.<Class<? extends Tombstone>>singleton(Tombstone.class);
		}

		@Override
		public Integer getId() {
			return Externalizers.TOMBSTONE;
		}

		@Override
		public void writeObject(ObjectOutput output, Tombstone tombstone) throws IOException {
			output.writeInt(tombstone.data.length);
			for (int i = 0; i < tombstone.data.length; ++i) {
				output.writeLong(tombstone.data[i]);
			}
		}

		@Override
		public Tombstone readObject(ObjectInput input) throws IOException, ClassNotFoundException {
			int length = input.readInt();
			long[] data = new long[length];
			for (int i = 0; i < data.length; ++i) {
				data[i] = input.readLong();
			}
			return new Tombstone(data);
		}
	}

	public static class ExcludeTombstonesFilter implements KeyValueFilter {
		private ExcludeTombstonesFilter() {}

		@Override
		public boolean accept(Object key, Object value, Metadata metadata) {
			return !(value instanceof Tombstone);
		}
	}

	public static class ExcludeTombstonesFilterExternalizer implements AdvancedExternalizer<ExcludeTombstonesFilter> {
		@Override
		public Set<Class<? extends ExcludeTombstonesFilter>> getTypeClasses() {
			return Collections.<Class<? extends ExcludeTombstonesFilter>>singleton(ExcludeTombstonesFilter.class);
		}

		@Override
		public Integer getId() {
			return Externalizers.EXCLUDE_TOMBSTONES_FILTER;
		}

		@Override
		public void writeObject(ObjectOutput output, ExcludeTombstonesFilter object) throws IOException {
		}

		@Override
		public ExcludeTombstonesFilter readObject(ObjectInput input) throws IOException, ClassNotFoundException {
			return EXCLUDE_TOMBSTONES;
		}
	}
}
