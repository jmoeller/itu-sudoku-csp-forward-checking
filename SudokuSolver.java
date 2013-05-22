import java.util.ArrayList;

public class SudokuSolver implements ISudokuSolver {

	int[][] puzzle;
	int size;
	ArrayList<ArrayList<Integer>> D; // = new ArrayList<ArrayList<Integer>>();

	public int[][] getPuzzle() {
		return puzzle;
	}

	public void setValue(int col, int row, int value) {
		puzzle[col][row] = value;
	}

	public void setup(int size1) {
		size = size1;

		int dimension_size = size * size;
		int number_of_elements = dimension_size * dimension_size;

		puzzle = new int[dimension_size][dimension_size];
		D = new ArrayList<ArrayList<Integer>>(number_of_elements);

		// Initialize each D[X]...

		// Valid values are [1; size*size] ([1; 9] for a regular sudoku puzzle)
		ArrayList<Integer> DefaultDimension = new ArrayList<Integer>(
				dimension_size);

		for (int i = 1; i <= dimension_size; i++) {
			DefaultDimension.add(i);
		}

		// Set the dimension for each of the elements in the sudoku puzzle
		for (int i = 0; i < number_of_elements; i++) {
			D.add(new ArrayList<Integer>(DefaultDimension));
		}
	}

	public boolean solve() {
		ArrayList<Integer> asn = GetAssignment(puzzle);

		// Determine if intial puzzle is valid..
		if (INITIAL_FC(asn)) {
			// .. and if it is, try to solve the puzzle
			asn = FC(asn);

			if (asn != null) {
				puzzle = GetPuzzle(asn);

				return true;
			}
		}

		// The puzzle cannot be solved either if:
		// The user has entered invalid values, or
		// The entered (valid) values just doesn't result in
		return false;
	}

	public void readInPuzzle(int[][] p) {
		puzzle = p;
	}

	// ---------------------------------------------------------------------------------
	// YOUR TASK: Implement FC(asn)
	// ---------------------------------------------------------------------------------
	public ArrayList<Integer> FC(ArrayList<Integer> asn) {
		if (!asn.contains(0)) {
			return asn;
		}

		// Find the first instance of a unassigned variable
		int X = asn.indexOf(0);

		// Backup the domains in case we need to roll back
		ArrayList<ArrayList<Integer>> D_old = Clone(D);

		// Create a "clean" copy of the domain for the X position to avoid
		// running into problems
		// with modifying the wrong one, which should be used in a later
		// iteration.
		ArrayList<Integer> DX_copy = new ArrayList<Integer>(D.get(X));
		for (int V : DX_copy) {
			// See if the value V at position X satisfies the constraints of the
			// sudoku
			if (AC_FC(X, V)) {

				// Try the value V at position X and make a recursive call
				// to see if it results in a valid solution.
				asn.set(X, V);
				ArrayList<Integer> R = FC(asn);

				if (R != null) {
					// A valid solution was found, so let's return that
					return R;
				}

				// This number didn't result in a valid configuration, so undo
				// the changes
				asn.set(X, 0);
				D = Clone(D_old);
			} else {
				D = Clone(D_old);
			}
		}

		return null;// failure
	}

	private ArrayList<ArrayList<Integer>> Clone(ArrayList<ArrayList<Integer>> D) {
		ArrayList<ArrayList<Integer>> clone = new ArrayList<ArrayList<Integer>>(
				D.size());

		for (ArrayList<Integer> domain : D) {
			clone.add(new ArrayList<Integer>(domain));
		}

		return clone;
	}

	// ---------------------------------------------------------------------------------
	// CODE SUPPORT FOR IMPLEMENTING FC(asn)
	//
	// It is possible to implement FC(asn) by using only AC_FC function from
	// below.
	//
	// If you have time, I strongly reccomend that you implement AC_FC and
	// REVISE from scratch
	// using only implementation of CONSISTENT algorithm and general utility
	// functions. In my opinion
	// by doing this, you will gain much more from this exercise.
	//
	// ---------------------------------------------------------------------------------

	// ------------------------------------------------------------------
	// AC_FC
	//
	// Implementation of acr-consistency for forward-checking AC-FC(cv).
	// This is a key component of FC algorithm, and the only function you need
	// to
	// use in your FC(asn) implementation
	// ------------------------------------------------------------------
	public boolean AC_FC(Integer X, Integer V) {
		// Reduce domain Dx
		D.get(X).clear();
		D.get(X).add(V);

		// Put in Q all relevant Y where Y>X
		ArrayList<Integer> Q = new ArrayList<Integer>(); // list of all relevant
															// Y
		int col = GetColumn(X);
		int row = GetRow(X);
		int cell_x = row / size;
		int cell_y = col / size;

		// all variables in the same column
		for (int i = 0; i < size * size; i++) {
			if (GetVariable(i, col) > X) {
				Q.add(GetVariable(i, col));
			}
		}
		// all variables in the same row
		for (int j = 0; j < size * size; j++) {
			if (GetVariable(row, j) > X) {
				Q.add(GetVariable(row, j));
			}
		}
		// all variables in the same size*size box
		for (int i = cell_x * size; i <= cell_x * size + 2; i++) {
			for (int j = cell_y * size; j <= cell_y * size + 2; j++) {
				if (GetVariable(i, j) > X) {
					Q.add(GetVariable(i, j));
				}
			}
		}

		// REVISE(Y,X)
		boolean consistent = true;
		while (!Q.isEmpty() && consistent) {
			Integer Y = (Integer) Q.remove(0);
			if (REVISE(Y, X)) {
				consistent = !D.get(Y).isEmpty();
			}
		}
		return consistent;
	}

	// ------------------------------------------------------------------
	// REVISE
	// ------------------------------------------------------------------
	public boolean REVISE(int Xi, int Xj) {
		Integer zero = new Integer(0);

		assert (Xi >= 0 && Xj >= 0);
		assert (Xi < size * size * size * size && Xj < size * size * size
				* size);
		assert (Xi != Xj);

		boolean DELETED = false;

		ArrayList<Integer> Di = D.get(Xi);
		ArrayList<Integer> Dj = D.get(Xj);

		for (int i = 0; i < Di.size(); i++) {
			Integer vi = (Integer) Di.get(i);
			ArrayList<Integer> xiEqVal = new ArrayList<Integer>(size * size
					* size * size);
			for (int var = 0; var < size * size * size * size; var++) {
				xiEqVal.add(var, zero);
			}

			xiEqVal.set(Xi, vi);

			boolean hasSupport = false;
			for (int j = 0; j < Dj.size(); j++) {
				Integer vj = (Integer) Dj.get(j);
				if (CONSISTENT(xiEqVal, Xj, vj)) {
					hasSupport = true;
					break;
				}
			}

			if (hasSupport == false) {
				Di.remove((Integer) vi);
				DELETED = true;
			}

		}

		return DELETED;
	}

	// ------------------------------------------------------------------
	// CONSISTENT:
	//
	// Given a partiall assignment "asn" checks whether its extension with
	// variable = val is consistent with Sudoku rules, i.e. whether it violates
	// any of constraints whose all variables in the scope have been assigned.
	// This implicitly encodes all constraints describing Sudoku.
	//
	// Before it returns, it undoes the temporary assignment variable=val
	// It can be used as a building block for REVISE and AC-FC
	//
	// NOTE: the procedure assumes that all assigned values are in the range
	// {0,..,9}.
	// -------------------------------------------------------------------
	public boolean CONSISTENT(ArrayList<Integer> asn, Integer variable,
			Integer val) {
		Integer v1, v2;

		// variable to be assigned must be clear
		assert (asn.get(variable) == 0);
		asn.set(variable, val);

		// alldiff(col[i])
		for (int i = 0; i < size * size; i++) {
			for (int j = 0; j < size * size; j++) {
				for (int k = 0; k < size * size; k++) {
					if (k != j) {
						v1 = (Integer) asn.get(GetVariable(i, j));
						v2 = (Integer) asn.get(GetVariable(i, k));
						if (v1 != 0 && v2 != 0 && v1.compareTo(v2) == 0) {
							asn.set(variable, 0);
							return false;
						}
					}
				}
			}
		}

		// alldiff(row[j])
		for (int j = 0; j < size * size; j++) {
			for (int i = 0; i < size * size; i++) {
				for (int k = 0; k < size * size; k++) {
					if (k != i) {
						v1 = (Integer) asn.get(GetVariable(i, j));
						v2 = (Integer) asn.get(GetVariable(k, j));
						if (v1 != 0 && v2 != 0 && v1.compareTo(v2) == 0) {
							asn.set(variable, 0);
							return false;
						}
					}
				}
			}
		}

		// alldiff(block[size*i,size*j])
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				for (int i1 = 0; i1 < size; i1++) {
					for (int j1 = 0; j1 < size; j1++) {
						int var1 = GetVariable(size * i + i1, size * j + j1);
						for (int i2 = 0; i2 < size; i2++) {
							for (int j2 = 0; j2 < size; j2++) {
								int var2 = GetVariable(size * i + i2, size * j
										+ j2);
								if (var1 != var2) {
									v1 = (Integer) asn.get(var1);
									v2 = (Integer) asn.get(var2);
									if (v1 != 0 && v2 != 0
											&& v1.compareTo(v2) == 0) {
										asn.set(variable, 0);
										return false;
									}
								}
							}
						}

					}
				}
			}
		}

		asn.set(variable, 0);
		return true;
	}

	// ------------------------------------------------------------------
	// INITIAL_FC
	// ------------------------------------------------------------------
	public boolean INITIAL_FC(ArrayList<Integer> anAssignment) {
		// Enforces consistency between unassigned variables and all
		// initially assigned values;
		for (int i = 0; i < anAssignment.size(); i++) {
			Integer V = (Integer) anAssignment.get(i);
			if (V != 0) {
				ArrayList<Integer> Q = GetRelevantVariables(i);
				boolean consistent = true;
				while (!Q.isEmpty() && consistent) {
					Integer Y = (Integer) Q.remove(0);
					if (REVISE(Y, i)) {
						consistent = !D.get(Y).isEmpty();
					}
				}
				if (!consistent)
					return false;
			}
		}

		return true;
	}

	// ------------------------------------------------------------------
	// GetRelevantVariables
	// ------------------------------------------------------------------
	public ArrayList<Integer> GetRelevantVariables(Integer X) {
		// Returns all variables that are interdependent of X, i.e.
		// all variables involved in a binary constraint with X
		ArrayList<Integer> Q = new ArrayList<Integer>(); // list of all relevant
															// Y
		int col = GetColumn(X);
		int row = GetRow(X);
		int cell_x = row / size;
		int cell_y = col / size;

		// all variables in the same column
		for (int i = 0; i < size * size; i++) {
			if (GetVariable(i, col) != X) {
				Q.add(GetVariable(i, col));
			}
		}
		// all variables in the same row
		for (int j = 0; j < size * size; j++) {
			if (GetVariable(row, j) != X) {
				Q.add(GetVariable(row, j));
			}
		}
		// all variables in the same size*size cell
		for (int i = cell_x * size; i <= cell_x * size + 2; i++) {
			for (int j = cell_y * size; j <= cell_y * size + 2; j++) {
				if (GetVariable(i, j) != X) {
					Q.add(GetVariable(i, j));
				}
			}
		}

		return Q;
	}

	// ------------------------------------------------------------------
	// Functions translating between the puzzle and an assignment
	// -------------------------------------------------------------------
	public ArrayList<Integer> GetAssignment(int[][] p) {
		ArrayList<Integer> asn = new ArrayList<Integer>();
		for (int i = 0; i < size * size; i++) {
			for (int j = 0; j < size * size; j++) {
				asn.add(GetVariable(i, j), new Integer(p[i][j]));
				if (p[i][j] != 0) {
					// restrict domain
					D.get(GetVariable(i, j)).clear();
					D.get(GetVariable(i, j)).add(new Integer(p[i][j]));
				}
			}
		}
		return asn;
	}

	public int[][] GetPuzzle(ArrayList asn) {
		int[][] p = new int[size * size][size * size];
		for (int i = 0; i < size * size; i++) {
			for (int j = 0; j < size * size; j++) {
				Integer val = (Integer) asn.get(GetVariable(i, j));
				p[i][j] = val.intValue();
			}
		}
		return p;
	}

	// ------------------------------------------------------------------
	// Utility functions
	// -------------------------------------------------------------------
	public int GetVariable(int i, int j) {
		assert (i < size * size && j < size * size);
		assert (i >= 0 && j >= 0);
		return (i * size * size + j);
	}

	public int GetRow(int X) {
		return (X / (size * size));
	}

	public int GetColumn(int X) {
		return X - ((X / (size * size)) * size * size);
	}

}
