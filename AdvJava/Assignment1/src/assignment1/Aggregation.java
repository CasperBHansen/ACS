package assignment1;

import java.util.List;

public interface Aggregation<T, R> {

	/**
	 * Compute the aggregate over a list by combining all elements with a given
	 * combination.
	 * 
	 * @param c
	 *            The combination to aggregate with (*).
	 * @param l
	 *            A list of values [e_1,...e_k].
	 * @return The aggregation get(e_1) * ... * get(e_k).
	 */
	public R aggregate(Combination<T, R> c, List<T> l);

}