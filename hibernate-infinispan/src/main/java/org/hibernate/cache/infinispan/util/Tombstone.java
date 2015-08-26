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
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class Tombstone {
	public static final ExcludeTombstonesFilter EXCLUDE_TOMBSTONES = new ExcludeTombstonesFilter();

	// when release == true and UUID is not found, don't insert anything, because this is a release delta
	private final boolean release;
	// the format of data is repeated (timestamp, UUID.LSB, UUID.MSB)
	private final long[] data;

	public Tombstone(UUID uuid, long timestamp, boolean release) {
		this.data = new long[] { timestamp, uuid.getLeastSignificantBits(), uuid.getMostSignificantBits() };
		this.release = release;
	}

	private Tombstone(long[] data, boolean release) {
		this.data = data;
		this.release = release;
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
		sb.append("release=").append(release);
		sb.append(", data={");
		for (int i = 0; i < data.length; i += 3) {
			if (i != 0) {
				sb.append(", ");
			}
			sb.append(new UUID(data[i + 2], data[i + 1])).append('=').append(data[i]);
		}
		sb.append("} }");
		return sb.toString();
	}

	public Tombstone merge(Tombstone old) {
		assert old != null;
		assert data.length == 3;
		if (release) {
			int toRemove = 0;
			for (int i = 0; i < old.data.length; i += 3) {
				if (old.data[i] < data[0] || (data[1] == old.data[i + 1] && data[2] == old.data[i + 2])) {
					toRemove += 3;
				}
			}
			if (old.data.length == toRemove) {
				// we want to remove all, but we need to keep at least ourselves
				return this;
			}
			else {
				long[] newData = new long[old.data.length - toRemove];
				int j = 0;
				for (int i = 0; i < old.data.length; i += 3) {
					if (old.data[i] >= data[0] && (data[1] != old.data[i + 1] || data[2] != old.data[i + 2])) {
						newData[j] = old.data[i];
						newData[j + 1] = old.data[i + 1];
						newData[j + 2] = old.data[i + 2];
						j += 3;
					}
				}
				return new Tombstone(newData, false);
			}
		}
		else {
			long[] newData = Arrays.copyOf(old.data, old.data.length + 3);
			System.arraycopy(data, 0, newData, old.data.length, 3);
			return new Tombstone(newData, false);
		}
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
			output.writeBoolean(tombstone.release);
			output.writeInt(tombstone.data.length);
			for (int i = 0; i < tombstone.data.length; ++i) {
				output.writeLong(tombstone.data[i]);
			}
		}

		@Override
		public Tombstone readObject(ObjectInput input) throws IOException, ClassNotFoundException {
			boolean release = input.readBoolean();
			int length = input.readInt();
			long[] data = new long[length];
			for (int i = 0; i < data.length; ++i) {
				data[i] = input.readLong();
			}
			return new Tombstone(data, release);
//			return INSTANCE;
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
