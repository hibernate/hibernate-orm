/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.util;

import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.filter.Converter;
import org.infinispan.metadata.Metadata;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * This value can be overwritten only by an entity with the same uuid
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class FutureUpdate {
	public static final ValueExtractor VALUE_EXTRACTOR = new ValueExtractor();

	private final UUID uuid;
	private final Object value;

	public FutureUpdate(UUID uuid, Object value) {
		this.uuid = uuid;
		this.value = value;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("FutureUpdate{");
		sb.append("uuid=").append(uuid);
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

	public static class Externalizer implements AdvancedExternalizer<FutureUpdate> {

		@Override
		public void writeObject(ObjectOutput output, FutureUpdate object) throws IOException {
			output.writeLong(object.uuid.getMostSignificantBits());
			output.writeLong(object.uuid.getLeastSignificantBits());
			output.writeObject(object.value);
		}

		@Override
		public FutureUpdate readObject(ObjectInput input) throws IOException, ClassNotFoundException {
			long msb = input.readLong();
			long lsb = input.readLong();
			return new FutureUpdate(new UUID(msb, lsb), input.readObject());
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

	public static class ValueExtractor implements Converter {
		private ValueExtractor() {}

		@Override
		public Object convert(Object key, Object value, Metadata metadata) {
			return value instanceof FutureUpdate ? ((FutureUpdate) value).getValue() : value;
		}
	}

	public static class ValueExtractorExternalizer implements AdvancedExternalizer<ValueExtractor> {
		@Override
		public Set<Class<? extends ValueExtractor>> getTypeClasses() {
			return Collections.<Class<? extends ValueExtractor>>singleton(ValueExtractor.class);
		}

		@Override
		public Integer getId() {
			return Externalizers.VALUE_EXTRACTOR;
		}

		@Override
		public void writeObject(ObjectOutput output, ValueExtractor object) throws IOException {
		}

		@Override
		public ValueExtractor readObject(ObjectInput input) throws IOException, ClassNotFoundException {
			return VALUE_EXTRACTOR;
		}
	}
}
