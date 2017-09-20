package sudoku;

import com.sun.org.apache.xpath.internal.operations.VariableSafeAbsRef;

import java.awt.*;
import java.lang.Boolean;
import java.lang.Exception;
import java.lang.IllegalStateException;
import java.lang.Integer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Place for your code.
 */
public class SudokuSolver {

	/**
	 * @return names of the authors and their student IDs (1 per line).
	 */
	public String authors() {
		return "Alexander Martinez (42504118)";
	}

	/**
	 * Performs constraint satisfaction on the given Sudoku board using Arc Consistency and Domain Splitting.
	 * 
	 * @param board the 2d int array representing the Sudoku board. Zeros indicate unfilled cells.
	 * @return the solved Sudoku board
	 */
	public int[][] solve(int[][] board) {
		// TODO write it;
		int[][][] tempBoard = domainBoard(board);

		int value = 1;

		while (value > 0) {
			value = 0;

			for (int i = 0; i < tempBoard.length; i++) {
				for (int j = 0; j < tempBoard[i].length; j++) {

					// Check if cell contains a fixed value
					if (tempBoard[i][j][0] == 0) {
						try {
							// Check contrainsts of both 3x3 grid and row/columns
							value += checkSubBoard(tempBoard, i , j);

							// If domain contains only one value, then fill cell with value
							clearDomain(tempBoard[i][j]);
							value += checkRowCol(tempBoard, i , j);
							clearDomain(tempBoard[i][j]);

						} catch (IllegalStateException e){
							throw new Exception("No solution exists to given board")
						}
					}

				}
			}

		}

		// Split domain if first pass of arc consistency doesn't produce solution
		if(!solved(tempBoard))
			tempBoard = domainSplitting(tempBoard, true);

		// If domain splitting cannot give a solution then no solution exists
		if(tempBoard == null)
			throw new Exception("Solution does not exist");

		// Verify there exists only one solution
		uniqueSolution(domainBoard(board), tempBoard);

		// If board is solved then produce 9x9 board
		for (int i = 0; i < tempBoard.length; i++)
			for (int j = 0; tempBoard[i].length; j++)
				board[i][j] = tempBoard[i][j][0];


		// Return solved board
		return board;
	}

	// Create new board with domain values
	public int[][][] domainBoard(int[][] board){
		// Create copy of board with storage for valid domain values
		int[][][] tempBoard = new int[9][9][10];

		// Creating tempBoard with domain values
		for (int i = 0; i < board.length; i++){
			for (int j = 0; j < board[i].length; ){
				tempBoard[i][j][0] = board[i][j];

				// If cell does not have value filled, then we add valid domain values
				if (tempBoard[i][j][0] == 0) {
					for (int k = 1; k < tempBoard[i][j].length; k++ )
						tempBoard[i][j][k] = k;
				}

			}
		}
		return tempBoard;

	}

	// Check constraints of 3x3 Grids
	public int checkSubBoard(int[][][] board, int row, int col){
		int n = 0;

		int startRow = (row/3)*3;
		int startCol = (col/3)*3;
		for(int i = 0; i < board.length; i++) {
			for (int j = 0; j < board[i].length,j++){

				// Check neighbour cells
				if (board[i][j] != 0 && (i != row && j != col)){

					// Checks if value can be pruned
					if (board[i][j][0] == board[row][col][0])
						throw new IllegalStateException();

					// Prune value from domain
					if (board[row][col][board[i][j][0]] != 0){
						board[row][col][board[i][j][0]] = 0;
						n = 1;
					}
				}


			}
		}
		return n;
	}

	// Check constrains of row and column of cell
	public int checkRowCol(int[][][] board, int row, int col){
		int n = 0;

		for(int i = 0; i < board.length; i++){

			// Check row
			if (board[row][i][0] != 0 && i != col){

				// Check if domain can be pruned
				if (board[row][i][0] == board[row][col][0]){
					throw new IllegalStateException();
				}
				// Prune value from domain
				if (board[row][col][board[row][i][col]] != 0){
					board[row][col][board[row][i][0]] = 0;
					n = 1;
				}
			}

			// Check column
			if (board[i][col][0] != 0 && i != row){

				// Check if domain can be pruned
				if (board[i][col][0] == board[row][col][0]){
					throw new IllegalStateException();
				}
				// Prune value from domain
				if (board[row][col][board[i][col][0]] != 0){
					board[row][col][board[i][col][0]] = 0;
					n = 1;
				}

			}
		}
		return n;
	}

	// Check if domain has only one value to put in cell
	public void clearDomain(int[] domain){
		// If cell is already filled
		if (domain[0] != 0)
			return;

		// If cell has more than 1 possible domain or domain is empty
		int a = 0;
		int b = 0;
		for (int n = 1; n <= 9; n++){
			if (domain[n] != 0){
				a++;
				b = n;
				if (a > 1)
					return;
			}
		}
		if (a == 0)
			return;

		domain[0] = domain[b];
	}

	// Verify that board has been solved
	public boolean solved(int[][][] board){

		// Check board is solved
		// Otherwise return false
		for (int i = 0; i < board.length; i++)
			for (int j = 0; j < board[i].length; j++)
				if (board[i][j][0] == 0)
					return false;

		// Board is solved
		return true;

	}

	// Operation to perform domain splitting
	// Must consider each possible value
	public int[][] domainSplitting(int[][][] board, boolean inOrder){
		int[] index = findEmpty(board);
		if (index[0] == -1) // Board is filled, return solution
			return board;

		int size = domainSize(board[index[0]][index[1]]);
		int leftDomain = size/2; // left domain size
		int rightDomain = size - leftDomain; // right domain size

		int[][][] temp = copyBoard(board);

		// perform AC on split domains, and if unsolved proceed recursively
		if (inOrder) {

			// Left domain
			for (int i = 0; i < 10; i++)


				if (temp[index[0]][index[1]][i] != 0) {
					leftDomain--;
					if (leftDomain < 0) temp[index[0]][index[1]][i] = 0;
				}

			try {

				// Perform Arc Consistency
				if (arcSearch(temp))
					return temp;

				temp = domainSplitting(temp, inOrder);

				if (temp != null)
					return temp;
			} catch (IllegalStateException e) {

			}

			// Right domain
			for (int i = 9; i >= 0; i--) {

				if (board[index[0]][index[1]][i] != 0) {
					rightDomain--;
					if (rightDomain < 0) board[index[0]][index[1]][i] = 0;
				}
			}
			try {

				// Perform Arc Consistency
				if (arcSearch(board))
					return board;

				board = domainSplitting(board, inOrder);

				if (board != null)
					return board;
			} catch (IllegalStateException e) {

			}
		}
		// Verify if split is taken is opposite order, we still produce same solution
		else {
			// Right domain
			for (int i = 9; i >= 0; i--) {
				if (board[index[0]][index[1]][i] != 0) {
					rightDomain--;
					if (rightDomain < 0) board[index[0]][index[1]][i] = 0;
				}
			}
			try {
				if (arcSearch(board))
					return board;
				board = domainSplitting(board, inOrder);
				if (board != null) return board;
			} catch (IllegalStateException e) {}

			// Left domain
			for (int i = 0; i < 10; i++)
				if (temp[index[0]][index[1]][i] != 0) {
					leftDomain--;
					if (leftDomain < 0) temp[index[0]][index[1]][i] = 0;
				}

			try {
				if (arcSearch(temp))
					return temp;
				temp = domainSplitting(temp, inOrder);
				if (temp != null) return temp;
			} catch (IllegalStateException e) {}
		}
		return null;
	}

	}

	// Perform arc consistency on domain split
	public boolean arcSearch(int[][][] board){
		int value = 1;
		while (value > 0) {
			value = 0;
			for (int i = 0; i < board.length; i++) {
				for (int j = 0; j < board[i].length; j++) {

					value += checkSubBoard(board, i, j);
					clearDomain(board[i][j]);

					value += checkRowCol(board, i, j);
					clearDomain(board[i][j]);

					// If domain size is reduced to 0, prune the domain
					if (domainSize(board[i][j]) == 0)
						throw new IllegalStateException("Empty domain");
				}
			}
		}

		return solved(board);

	}

	// Find unfilled cell
	public int[] findEmpty(int[][][] board){
		int[] index = new int[2];
		for (int i = 0; i < board.length; i++)
			for (int j = 0; j < board[i].length; j++)
				if (board[i][j][0] == 0) {
					index[0] = i;
					index[1] = j;
					return index;
				}
		// didn't find an empty cell, perhaps a solution
		index[0] = -1;
		return index;

	}

	// Produces size of the domain
	public int domainSize(int[] dom){
		int a = 0;
		for (int n = 0; n <= 9; n++)
			if (dom[n]!=0)
				a++;
		return a;

	}

	// Make a copy of a domain board
	public int[][][] copyBoard(int[][][] board) {
		int[][][] boardCopy = new int[9][9][10];
		for (int i = 0; i < board.length; i++)
			for (int j = 0; j < board[i].length; j++)
				for (int k = 0; k < board[i][j].length; k++)
					boardCopy[i][j][k] = board[i][j][k];
		return boardCopy;

	}

	// Verify that order of recursion does not effect solution
	public void uniqueSolution(int[][][] board; int[][][] sol){
		board = domainSplitting(board, false);
		for (int i = 0; i < board.length; i++)
			for (int j = 0; j < board[i].length; j++)
				for (int k = 0; k < board[i][j].length; k++)
					if (board[i][j][k] != solution[i][j][k])
						throw new Exception("There does not exist a unqiue solution to this board");

	}




}
