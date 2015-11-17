package assignment1;

import java.util.List;

public interface ListMutation<T> {

	/**
	 * A list mutation. Mutates each element in a list by a given mutation.
	 * 
	 * @param m
	 *            The mutation.
	 * @param l
	 *            The list to mutate.
	 */
	public void listMutate(Mutation<T> m, List<T> l);

}
