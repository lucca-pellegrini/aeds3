package AEDs3.Util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class Range implements Iterable<Integer> {
	private int min;
	private int max;

	public Range(int max) {
		this.min = 0;
		this.max = max;
	}

	public Range(int min, int max) {
		this.min = min;
		this.max = max;
	}

	@Override
	public Iterator<Integer> iterator() {
		return new Iterator<Integer>() {
			private int current = min;

			@Override
			public boolean hasNext() {
				return current < max;
			}

			@Override
			public Integer next() {
				if (hasNext())
					return current++;
				else
					throw new NoSuchElementException("Range chegou ao fim");
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException(
						"Não é possível remover valores de um Range");
			}
		};
	}
}
