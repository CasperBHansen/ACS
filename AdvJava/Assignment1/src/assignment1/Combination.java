package assignment1;

public interface Combination<T, R> {

	/**
	 * combine(x, neutral()) = x = combine(neutral(), x).
	 * 
	 * @return The neutral element of the combination.
	 */
	public R neutral();

	/**
	 * 
	 * combine(combine(x,y),z) = combine(x,combine(y,z))
	 * 
	 * @return The combination of x and y.
	 */
	public R combine(R x, R y);

	/**
	 * @param x An element.
	 * 
	 * @return A projected result value.
	 */
	public R get(T x);
}
