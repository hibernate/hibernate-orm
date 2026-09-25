package org.hibernate.testing.memory;

interface MemoryAllocationSnapshot {
	long difference(MemoryAllocationSnapshot before);
}
